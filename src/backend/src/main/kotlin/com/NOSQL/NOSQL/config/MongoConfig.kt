package com.NOSQL.NOSQL.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.data.mongodb.gridfs.GridFsTemplate


@Configuration
class MongoConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${app.mongodb.gridfs-bucket:media}")
    private lateinit var gridFsBucket: String

    @Value("\${spring.data.mongodb.uri:mongodb://localhost:27017/nosql_db}")
    private lateinit var mongoUri: String

    @Value("\${spring.data.mongodb.database:nosql_db}")
    private lateinit var databaseName: String

    @Bean
    @Primary
    @ConditionalOnProperty(name = ["spring.data.mongodb.uri"])
    fun mongoClient(): MongoClient {
        log.info("MongoDB: creating client from URI (database will be '{}')", databaseName)
        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(mongoUri))
            .build()
        return MongoClients.create(settings)
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = ["spring.data.mongodb.uri"])
    fun mongoDatabaseFactory(mongoClient: MongoClient): MongoDatabaseFactory {
        log.info("MongoDB: using database '{}'", databaseName)
        return SimpleMongoClientDatabaseFactory(mongoClient, databaseName)
    }

    @Bean
    fun gridFsTemplate(
        mongoDbFactory: MongoDatabaseFactory,
        mongoConverter: MongoConverter
    ): GridFsTemplate =
        GridFsTemplate(mongoDbFactory, mongoConverter, gridFsBucket)
}
