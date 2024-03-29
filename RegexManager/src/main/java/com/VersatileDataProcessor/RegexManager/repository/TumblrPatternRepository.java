package com.versatileDataProcessor.regexManager.repository;

import com.versatileDataProcessor.regexManager.models.TumblrPattern;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface TumblrPatternRepository extends MongoRepository<TumblrPattern, String> {
    @Query("{expression: '?0'}")
    TumblrPattern findPatternByExpression(String expression);

    long count();
}
