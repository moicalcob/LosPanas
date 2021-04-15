package com.gotacar.backend.models.paymentReturn;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentReturnRepository extends MongoRepository<PaymentReturn, String> {
    public List<PaymentReturn> findByStatus(String status);
}