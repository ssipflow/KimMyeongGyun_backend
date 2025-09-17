package com.moneyTransfer.api.mapper;

import com.moneyTransfer.api.dto.request.CreateAccountApiRequest;
import com.moneyTransfer.api.dto.response.AccountApiResponse;
import com.moneyTransfer.application.dto.account.AccountResponse;
import com.moneyTransfer.application.dto.account.CreateAccountRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AccountDtoMapper {

    public CreateAccountRequest toApplicationRequest(CreateAccountApiRequest apiRequest) {
        return new CreateAccountRequest(
                apiRequest.getUserName(),
                apiRequest.getEmail(),
                apiRequest.getIdCardNo(),
                apiRequest.getBankCode(),
                apiRequest.getAccountNo()
        );
    }

    public AccountApiResponse toApiResponse(AccountResponse applicationResponse) {
        return new AccountApiResponse(
                applicationResponse.getId(),
                applicationResponse.getUserId(),
                applicationResponse.getBankCode(),
                applicationResponse.getAccountNo(),
                applicationResponse.getBalance(),
                applicationResponse.getStatus().name(), // Enum을 String으로 변환
                applicationResponse.getCreatedAt(),
                null // deactivatedAt은 AccountResponse에 없으므로 null
        );
    }

    public List<AccountApiResponse> toApiResponseList(List<AccountResponse> applicationResponses) {
        return applicationResponses.stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList());
    }
}