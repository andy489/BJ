package com.casino.blackjack.controller;

import com.casino.blackjack.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController extends BaseController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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
}
