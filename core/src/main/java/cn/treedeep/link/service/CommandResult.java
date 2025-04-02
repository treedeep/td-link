package cn.treedeep.link.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
public class CommandResult {
    private boolean success;
    private String message;
    private LocalDateTime timestamp;

    public static CommandResult success() {
        return new CommandResult(true, "指令发送成功", LocalDateTime.now());
    }

    public static CommandResult failure(String message) {
        return new CommandResult(false, message, LocalDateTime.now());
    }
}