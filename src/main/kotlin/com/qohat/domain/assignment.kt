package com.qohat.domain

data class Assignment(val userId: UserId, val assignments: Long)
data class RequestAssignment(val userId: UserId, val peopleRequestId: PeopleRequestId)