package com.flab.tiple.global.exception;


import lombok.Getter;

@Getter
public enum ErrorCode {
    MEMBER_DUPLICATED("이 유저는 이미 가입된 멤버입니다", 400),
    MEMBER_NOT_FOUND("유저를 찾을 수 없습니다", 400),
    MEMBER_NOT_MATCH("유저정보가 일치하지 않습니다",400),

    //콘서트 관련 에러
    CONCERT_NOT_FOUND("콘서트를 찾을 수 없습니다", 400),
    CONCERT_SEAT_NOT_FOUND("콘서트 좌석을 찾을 수 없습니다",400),
    CONCERT_SEAT_RESERVATION_NOT_POSSIBLE("해당 좌석은 예매가 불가능합니다",400),
    CONCERT_CLOSED("콘서트를 예매할 수 없습니다",400),
    CONCERT_NOT_START_TIME_RESERVATION("콘서트 예매시간 시작이 안되었습니다",400),
    CONCERT_LIMIT_END_TIME_RESERVATION("콘서트 예매 종료시간이 지났습니다",400),
    CONCERT_CANCEL_TIME_EXCEED("콘서트 취소 가능 시간이 초과했습니다.",400),
    CONCERT_REMAINING_SEAT_EXIST("다른 좌석이 아직 가용합니다. 다른 좌석을 선택해주세요.",400),

    //티켓 관련 에러
    TICKET_RESERVATION_NOT_FOUND("예약된 티켓을 찾을 수 없습니다",400),
    TICKET_RESERVATION_NOT_POSSIBLE_STATUS("티켓이 예매 가능한 상태가 아닙니다",400),
    TICKET_RESERVATION_NOT_CANCEL_STATUS("티켓이 취소 가능한 상태가 아닙니다",400),

    //티켓 웨이팅 관련 에러
    TICKET_WAITING_ERROR("티켓팅 예약 중 에러가 발생했습니다.",400),
    TICKET_WAITING_NOT_FOUND("티켓팅 웨이팅을 찾지 못했습니다.",400),
    TICKET_WAITING_INVALID_WAITING_STATUS("티켓 웨이팅 상태가 잘못되었습니다",400),

    // 인증관련에러
    UNAUTHORIZED("UNAUTHORIZED", 401),
    TOKEN_EXPIRED("TOKEN_EXPIRED.",401),
    TOKEN_INVALID("TOKEN_INVALID.",401),
    TOKEN_MALFORMED("TOKEN_MALFORMED.",401),
    TOKEN_ERROR("TOKEN_ERROR.",401);

    private final String description;
    private final int status;

    ErrorCode(String description, int status) {
        this.description = description;
        this.status = status;
    }
}
