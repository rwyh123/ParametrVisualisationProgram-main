package org.example.ETL.service;

import org.example.DB.write.DbWriteService; // Используем Write сервис
import org.example.ETL.entities.*;
import org.example.ETL.util.Snapshot;
import org.example.ETL.util.UnitResolverExistingOnly;
import org.example.Events.AddEvent.NewIndicatorsIdsEvent; // Новое событие
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;

public class MtconnectIngestService {

    private static final String NS = "urn:mtconnect.org:MTConnectStreams:1.8";

    private final MtconnectReaderService reader;
    private final UnitResolverExistingOnly unitResolver;
    private final DbWriteService dbWrite; // Только запись
    private final ApplicationEventPublisher eventPublisher;
    private final String fallbackMachineName;

    private volatile String lastProcessedHash;

    public MtconnectIngestService(MtconnectReaderService reader,
                                  UnitResolverExistingOnly unitResolver,
                                  DbWriteService dbWrite,
                                  String machineName,
                                  ApplicationEventPublisher eventPublisher) {
        this.reader = reader;
        this.unitResolver = unitResolver;
        this.dbWrite = dbWrite;
        this.eventPublisher = eventPublisher;
        this.fallbackMachineName = (machineName != null && !machineName.isBlank())
                ? machineName
                : "DemoMachine";
    }

    @Scheduled(fixedDelayString = "${mtconnect.ingest-ms:3000}")
    public void runIngest() {
        Snapshot<String> snap = reader.getLastSnapshot();
        if (snap == null || !snap.isOk()) return;

        String body = snap.getBody();
        if (body == null || body.isBlank()) return;

        String hash = sha256(body);
        if (hash.equals(lastProcessedHash)) return;

        ingestXml(body);
        lastProcessedHash = hash;
    }

    public void ingestOnceFromRawXml(String xml) {
        if (xml == null || xml.isBlank()) return;
        ingestXml(xml);
    }

    void ingestXml(String xml) {
        try {
            Document doc = parse(xml);

            String xmlMachineName = attr(doc, "/s:MTConnectStreams/s:Streams/s:DeviceStream/@name");
            String effectiveMachine = (xmlMachineName != null && !xmlMachineName.isBlank())
                    ? xmlMachineName
                    : fallbackMachineName;

            Machine machine = dbWrite.getOrCreateMachine(effectiveMachine);

            // 1. Создаем список для сбора ID
            List<Integer> newIndicatorIds = new ArrayList<>();

            NodeList componentStreams = xp(doc, "/s:MTConnectStreams/s:Streams/s:DeviceStream/s:ComponentStream");
            for (int i = 0; i < componentStreams.getLength(); i++) {
                Element cs = (Element) componentStreams.item(i);
                String compName = cs.getAttribute("name");
                String compType = cs.getAttribute("component");
                String deviceName = (compName != null && !compName.isBlank()) ? compName : compType;

                Device device = dbWrite.getOrCreateDevice(machine.getId(), deviceName);
                Map<String, Integer> idCounters = new HashMap<>();

                // Samples
                NodeList samples = xp(cs, "s:Samples/*");
                for (int j = 0; j < samples.getLength(); j++) {
                    Element e = (Element) samples.item(j);
                    String effectiveId = resolveEffectiveId(e, idCounters);
                    Indicator ind = persistIndicatorIfPossible(device, e, effectiveId);
                    if (ind != null) newIndicatorIds.add(ind.getId());
                }

                // Events
                NodeList events = xp(cs, "s:Events/*");
                for (int j = 0; j < events.getLength(); j++) {
                    Element e = (Element) events.item(j);
                    String effectiveId = resolveEffectiveId(e, idCounters);
                    Indicator ind = persistIndicatorIfPossible(device, e, effectiveId);
                    if (ind != null) newIndicatorIds.add(ind.getId());
                }
            }

            // 2. Отправляем событие с ID
            if (!newIndicatorIds.isEmpty()) {
                eventPublisher.publishEvent(new NewIndicatorsIdsEvent(newIndicatorIds));
            }

        } catch (Exception ex) {
            System.out.println("[ingest] Error: " + ex);
            ex.printStackTrace();
        }
    }

    // Возвращаем Indicator, чтобы взять его ID
    private Indicator persistIndicatorIfPossible(Device device, Element e, String effectiveId) {
        String tag = e.getTagName();
        String nativeUnits = e.getAttribute("nativeUnits");
        String ts = e.getAttribute("timestamp");
        String txt = e.getTextContent();
        String dataItemIdRaw = e.getAttribute("dataItemId");

        DataItem dataItem = dbWrite.getOrCreateDataItem(device, effectiveId, tag);

        Optional<Unit> unitOpt = unitResolver.resolve(tag, nativeUnits, dataItemIdRaw);
        if (unitOpt.isEmpty()) return null;

        OffsetDateTime time = null;
        try { if (ts != null && !ts.isBlank()) time = OffsetDateTime.parse(ts); } catch (Exception ignored) {}
        if (time == null) time = OffsetDateTime.now();

        Double val = null;
        String textVal = null;
        if (txt != null && !txt.isBlank()) {
            String trimmed = txt.trim();
            try {
                BigDecimal bd = new BigDecimal(trimmed);
                val = bd.doubleValue();
            } catch (Exception ignored) {
                textVal = trimmed;
            }
        }

        return dbWrite.saveIndicator(dataItem, unitOpt.get(), time, val, textVal);
    }

    private String resolveEffectiveId(Element e, Map<String, Integer> counters) {
        String dataItemId = e.getAttribute("dataItemId");
        if (dataItemId != null && !dataItemId.isBlank()) return dataItemId;
        String tag = e.getTagName();
        int count = counters.getOrDefault(tag, 0) + 1;
        counters.put(tag, count);
        return tag + "_" + count;
    }

    // ... XML helpers (parse, xp, attr, Ns, sha256) без изменений ...
    private static Document parse(String xml) throws Exception {
        var dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
    private static NodeList xp(Node ctx, String expr) {
        try {
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xp = xpf.newXPath();
            xp.setNamespaceContext(new Ns());
            return (NodeList) xp.compile(expr).evaluate(ctx, XPathConstants.NODESET);
        } catch (Exception e) { throw new RuntimeException("XPath error: " + expr, e); }
    }
    private static String attr(Node ctx, String expr) {
        try {
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xp = xpf.newXPath();
            xp.setNamespaceContext(new Ns());
            String v = (String) xp.compile(expr).evaluate(ctx, XPathConstants.STRING);
            return (v != null && !v.isBlank()) ? v : null;
        } catch (Exception e) { throw new RuntimeException("XPath attr error: " + expr, e); }
    }
    private static final class Ns implements NamespaceContext {
        public String getNamespaceURI(String prefix) {
            if ("s".equals(prefix)) return NS;
            if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
            return XMLConstants.NULL_NS_URI;
        }
        public String getPrefix(String nsURI) { return null; }
        public Iterator<String> getPrefixes(String nsURI) { return null; }
    }
    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format(Locale.ROOT, "%02x", b));
            return sb.toString();
        } catch (Exception e) { return Integer.toHexString(Objects.hashCode(s)); }
    }
}