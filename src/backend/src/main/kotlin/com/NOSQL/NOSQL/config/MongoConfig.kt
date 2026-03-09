package com.NOSQL.NOSQL.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.data.mongodb.gridfs.GridFsTemplate


@Configuration
class MongoConfig {

    @Value("\${app.mongodb.gridfs-bucket:media}")
    private lateinit var gridFsBucket: String

    @Bean
    fun gridFsTemplate(
        mongoDbFactory: MongoDatabaseFactory,
        mongoConverter: MongoConverter
    ): GridFsTemplate =
        GridFsTemplate(mongoDbFactory, mongoConverter, gridFsBucket)
}
