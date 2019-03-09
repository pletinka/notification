
package com.example.demo.contoller;

import com.example.demo.dto.MsgDto;
import com.example.demo.service.MsgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class MsgController {

    @Autowired
    private MsgService msgService;

    @RequestMapping("/msg")
    public ResponseEntity addMsg(@RequestBody MsgDto dto) {
        ResponseEntity responseEntity;
        if (isValid(dto)) {
            msgService.addMsg(dto);
            responseEntity = ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            responseEntity = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return responseEntity;
    }

    private boolean isValid(MsgDto dto) {
        return dto != null && dto.getMsg() != null && LocalDateTime.now().isBefore(dto.getTime());
    }
}
