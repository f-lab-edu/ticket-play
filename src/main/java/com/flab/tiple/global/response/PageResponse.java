package com.flab.tiple.global.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PageResponse<T> {
	private List<T> content;   // 실제 데이터 리스트
	private int currentPage;   // 현재 페이지 번호
	private int totalPages;    // 전체 페이지 개수
	private long totalElements; // 전체 데이터 개수
	private boolean hasNext;   // 다음 페이지 존재 여부
}

