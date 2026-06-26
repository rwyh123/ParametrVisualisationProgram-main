package org.example.Server.controller;

import org.example.ETL.config.ThresholdConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    private final ThresholdConfig thresholdConfig;

    @Autowired
    public WebController(ThresholdConfig thresholdConfig) {
        this.thresholdConfig = thresholdConfig;
    }

    // При заходе на корень перенаправляем на дашборд
    @GetMapping("/")
    public String index() {
        return "redirect:/cnc";
    }

    // Основная страница мониторинга
    @GetMapping("/cnc")
    public String cncPage(Model model) {
        model.addAttribute("title", "CNC MONITORING SYSTEM");
        // Передаем карту порогов во фронтенд
        model.addAttribute("thresholds", thresholdConfig.getThresholds());
        return "cnc"; // Ищет cnc.html в src/main/resources/templates/
    }
}