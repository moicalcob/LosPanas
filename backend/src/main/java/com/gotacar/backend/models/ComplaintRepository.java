package com.gotacar.backend.models;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ComplaintRepository extends MongoRepository<Complaint, String> {
    public Optional<Complaint> findById(String id);

    public Complaint findById(ObjectId id);

}
