package com.example.server

import java.io.Serializable

interface Room {
    fun put(ip: String, guest: Guest)
    fun getAllRoomGuests(ip: String): List<Guest>
    fun getGuest(id: Serializable): Guest?
    fun kickOutGuest(id: Serializable)
    fun kickOutGuest(ip: String, id: Serializable)
    fun reportSituation()
}


interface Guest {
    fun identification(): Serializable
}


class GuestImpl : Guest {

    var id: Serializable? = null

    var tempName: String? = null

    var ip: String? = null

    override fun identification(): Serializable {
        return id!!
    }

    override fun toString(): String {
        return "GuestImpl(id=$id, tempName=$tempName, ip=$ip)"
    }
}

class PoorRoom: Room {

}