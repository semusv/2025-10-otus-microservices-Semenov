package com.microservices.warehouse.controller;

import com.microservices.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final WarehouseService warehouseService;

    //    @PostMapping("/order")
    //    public BillingBalanceResponse withdraw(
    //            @RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody BillingOrderRequest orderRequest) {
    //        return accountService.orderPayment(userId, orderRequest);
    //    }
}
