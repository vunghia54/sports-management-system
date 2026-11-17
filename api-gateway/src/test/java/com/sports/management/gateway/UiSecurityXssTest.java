package com.sports.management.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiSecurityXssTest {

    @Test
    void usersTableResetPasswordButton_usesDataAttributesWithoutInlineJsUsernameInterpolation() throws Exception {
        String appJs = new String(
                new ClassPathResource("static/js/app.js").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertFalse(appJs.contains("onclick=\"openAdminResetPasswordModal"),
                "Admin reset password button must not interpolate username into inline onclick JS");
        assertTrue(appJs.contains("data-action=\"admin-reset-password\""),
                "Reset password button should use data-action attribute");
        assertTrue(appJs.contains("data-username=\"${escapeHtml(u.username)}\""),
                "Username should be stored safely in data-username attribute");
        assertTrue(appJs.contains(".replace(/'/g, '&#39;')"),
                "escapeHtml should escape single quotes as well");
    }
}
