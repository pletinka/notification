
package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MsgDto {
    private LocalDateTime time;
    private String msg;
}
