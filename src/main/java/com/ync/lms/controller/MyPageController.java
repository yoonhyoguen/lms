package com.ync.lms.controller;

import com.ync.lms.domain.User;
import com.ync.lms.dto.PasswordChangeForm;
import com.ync.lms.dto.ProfileForm;
import com.ync.lms.exception.FieldValidationException;
import com.ync.lms.security.LoginUser;
import com.ync.lms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 추가 기능: 내 정보 (이름 변경, 비밀번호 변경). 강사·학생 공통
 */
@Controller
@RequestMapping("/me")
@RequiredArgsConstructor
public class MyPageController {

    private final UserService userService;

    // 세션에 로그인 정보를 다시 저장할 때 사용 (Spring Security 6 은 직접 저장해야 함)
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping
    public String myPage(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        ProfileForm profileForm = new ProfileForm();
        profileForm.setName(userService.getUser(loginUser.getId()).getName());
        return render(loginUser, profileForm, new PasswordChangeForm(), model);
    }

    // 이름 변경
    @PostMapping("/profile")
    public String changeName(@AuthenticationPrincipal LoginUser loginUser,
                             @Valid @ModelAttribute ProfileForm profileForm,
                             BindingResult bindingResult,
                             Model model,
                             HttpServletRequest request,
                             HttpServletResponse response,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return render(loginUser, profileForm, new PasswordChangeForm(), model);
        }
        User user = userService.changeName(loginUser.getId(), profileForm);

        // 세션의 LoginUser 는 로그인할 때 복사한 값이라, 새 이름으로 다시 만들어 넣어야 상단바에 바로 반영됨
        refreshLoginUser(user, request, response);

        redirectAttributes.addFlashAttribute("message", "이름을 변경했습니다.");
        return "redirect:/me";
    }

    // 비밀번호 변경
    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                 @Valid @ModelAttribute PasswordChangeForm passwordChangeForm,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                userService.changePassword(loginUser.getId(), passwordChangeForm);
                redirectAttributes.addFlashAttribute("message", "비밀번호를 변경했습니다. 다음 로그인부터 새 비밀번호를 사용하세요.");
                return "redirect:/me";
            } catch (FieldValidationException e) {
                bindingResult.rejectValue(e.getField(), "invalid", e.getMessage());
            }
        }
        ProfileForm profileForm = new ProfileForm();
        profileForm.setName(userService.getUser(loginUser.getId()).getName());
        return render(loginUser, profileForm, passwordChangeForm, model);
    }

    private String render(LoginUser loginUser, ProfileForm profileForm, PasswordChangeForm passwordForm, Model model) {
        model.addAttribute("user", userService.getUser(loginUser.getId()));
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("passwordChangeForm", passwordForm);
        return "me";
    }

    private void refreshLoginUser(User user, HttpServletRequest request, HttpServletResponse response) {
        LoginUser updated = new LoginUser(user);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(updated, null, updated.getAuthorities()));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
