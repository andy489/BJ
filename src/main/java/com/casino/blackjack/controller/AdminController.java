package com.casino.blackjack.controller;

import com.casino.blackjack.service.AdminService;
import com.casino.blackjack.service.mail.MailService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Locale;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController extends BaseController {

    private final AdminService adminService;
    private final MailService mailService;

    public AdminController(AdminService adminService, MailService mailService) {
        this.adminService = adminService;
        this.mailService = mailService;
    }

    @GetMapping
    public ModelAndView panel(ModelAndView mav) {
        return super.view("admin/admin", mav);
    }

    @PostMapping("/clear-history")
    public ModelAndView clearHistory(ModelAndView mav) {
        adminService.clearAllHistory();
        mav.addObject("cleared", true);
        return super.view("admin/admin", mav);
    }

    @GetMapping("/email-preview")
    public ResponseEntity<String> emailPreview(@RequestParam(defaultValue = "activation") String type,
                                               Locale locale) {
        String html = type.equals("reset-pass")
                ? mailService.previewForgotPassEmail(locale)
                : mailService.previewActivationEmail(locale);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
