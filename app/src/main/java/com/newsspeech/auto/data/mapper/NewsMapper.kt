//package com.newsspeech.auto.data.mapper
//
//import com.newsspeech.auto.data.remote.NewsRealmObject
//import com.newsspeech.auto.domain.model.News
//
///**
// * Mapper: Convert Realm Object -> Domain Model
// *
// * Chuyển đổi dữ liệu từ MongoDB Realm sang model News của app
// */
//fun NewsRealmObject.toDomain(): News {
//    return News(
//        id = this.id,
//        title = this.title,
//        content = this.content,
//        link = this.link,
//        timestamp = this.timestamp,
//        source = this.source,
//        category = this.category
//    )
//}
//
///**
// * Extension function để convert List<NewsRealmObject> -> List<News>
// */
//fun List<NewsRealmObject>.toDomainList(): List<News> {
//    return this.map { it.toDomain() }
//}