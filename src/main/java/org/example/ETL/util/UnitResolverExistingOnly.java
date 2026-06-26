package org.example.ETL.util;

import org.example.ETL.entities.Unit;

import java.util.Locale;
import java.util.Optional;

public class UnitResolverExistingOnly {

    private final UnitCatalog catalog;

    public UnitResolverExistingOnly(UnitCatalog catalog) {
        this.catalog = catalog;
    }

    public Optional<Unit> resolve(String tag, String nativeUnits, String dataItemId) {
        // 1) сначала по nativeUnits → short_name
        String s = mapNativeToShort(nativeUnits);
        if (s != null) {
            Optional<Unit> u = catalog.byShort(s);
            if (u.isPresent()) return u;
        }

        // 2) по dataItemId
        s = mapDataItemToShort(dataItemId);
        if (s != null) {
            Optional<Unit> u = catalog.byShort(s);
            if (u.isPresent()) return u;
        }

        // 3) по имени тега (Temperature / Text / Event ...)
        s = mapTagToShort(tag);
        if (s != null) {
            Optional<Unit> u = catalog.byShort(s);
            if (u.isPresent()) return u;
        }

        // 4) Fallback: если ничего не подошло, пробуем "-" (No unit)
        return catalog.byShort("-");
    }

    private String mapNativeToShort(String nativeUnits) {
        if (nativeUnits == null) return null;
        String nu = nativeUnits.trim().toUpperCase(Locale.ROOT);
        return switch (nu) {
            case "AMPERE"        -> "A";
            case "VOLT"          -> "V";
            case "WATT"          -> "W";
            case "DEGREE"        -> "deg";
            case "NEWTON_METER"  -> "N·m";
            case "PERCENT"       -> "%";
            case "BAR"           -> "bar";
            case "PASCAL"        -> "Pa";
            // Можно добавить MILLIMETER, SECOND и т.д.
            default              -> null;
        };
    }

    private String mapDataItemToShort(String dataItemId) {
        if (dataItemId == null) return null;
        String id = dataItemId.toLowerCase(Locale.ROOT);
        if (id.contains("rpm")) return "rpm";
        if (id.endsWith("_temp") || id.contains("temperature")) return "°C";
        if (id.endsWith("_mcs") || id.endsWith("_wcs") || id.contains("position")) return "mm";
        return null;
    }

    private String mapTagToShort(String tag) {
        if (tag == null) return null;
        return switch (tag) {
            case "Temperature"     -> "°C";
            case "RotaryVelocity"  -> "rpm";
            case "Position"        -> "mm";

            // ДОБАВЛЕНО: Явное указание для текстовых и событийных тегов
            case "Text"            -> "-"; // No unit
            case "Event"           -> "-"; // No unit
            case "Message"         -> "-"; // Часто встречается в MTConnect

            default                -> null;
        };
    }
}