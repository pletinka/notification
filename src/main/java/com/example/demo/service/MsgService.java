package com.example.demo.service;

import com.example.demo.dto.MsgDto;


public interface MsgService {
    void addMsg(MsgDto userDtoReq);

    void pop();

    void pull();
}
