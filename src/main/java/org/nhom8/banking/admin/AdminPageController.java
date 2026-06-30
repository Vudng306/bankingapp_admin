package org.nhom8.banking.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping({"/admin", "/admin/"})
    public String adminIndex() {
        return "redirect:/admin/index.html";
    }
}
