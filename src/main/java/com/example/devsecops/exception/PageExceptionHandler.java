package com.example.devsecops.exception;

import com.example.devsecops.controller.PageController;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Renders a friendly error page for server-rendered routes. */
@ControllerAdvice(assignableTypes = PageController.class)
public class PageExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PageExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    String notFound(NotFoundException e, Model model, HttpServletResponse res) {
        return render(model, res, HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    String badId(Model model, HttpServletResponse res) {
        return render(model, res, HttpStatus.BAD_REQUEST, "The requested identifier is not valid.");
    }

    @ExceptionHandler(Exception.class)
    String unexpected(Exception e, Model model, HttpServletResponse res) {
        log.error("Unexpected error while rendering page", e);
        return render(model, res, HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong while loading this page.");
    }

    private String render(Model model, HttpServletResponse res, HttpStatus status, String message) {
        res.setStatus(status.value());
        model.addAttribute("status", status.value());
        model.addAttribute("message", message);
        return "error";
    }
}
