package com.template.flows.utils

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import java.io.File
import java.io.InputStream
import java.security.PublicKey

object FlowUtils {
    @Suspendable
    fun strToX500Name(x500Name: String): CordaX500Name {
        var org = ""
        var orgUnit: String? = null
        var location = ""
        var country = ""

        val strArr = x500Name.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in strArr.indices) {
            val items = strArr[i].split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (items.size < 2) continue
            when (items[0]) {
                "O" -> org = items[1]
                "OU" -> orgUnit = items[1]
                "L" -> location = items[1]
                "C" -> country = items[1]
            }
        }

        return if (orgUnit == null || orgUnit.isEmpty()) {
            CordaX500Name(null, null, org, location, null, country)
        } else {
            CordaX500Name(null, orgUnit, org, location, null, country)
        }
    }

    @Suspendable
    fun x500NameToStr(myX500Name: CordaX500Name): String {
        val orgUnit = myX500Name.organisationUnit
        return if (orgUnit == null || orgUnit.isEmpty()) {
            "O=${myX500Name.organisation},L=${myX500Name.locality},C=${myX500Name.country}"
        } else {
            "OU=${myX500Name.organisationUnit},O=${myX500Name.organisation},L=${myX500Name.locality},C=${myX500Name.country}"
        }
    }

    @Suspendable
    fun getNotary(serviceHub: ServiceHub, notaryName: String = ""): Party? {
        return if (notaryName.isNotEmpty())
            serviceHub.networkMapCache.getNotary(this.strToX500Name(notaryName))
        else
            serviceHub.networkMapCache.notaryIdentities.getOrNull(0)
    }

    @Suspendable
    fun getParty(serviceHub: ServiceHub, name: String): Party? {
        return serviceHub.networkMapCache.getPeerByLegalName(this.strToX500Name(name))
    }

    @Suspendable
    fun uploadAttachment(
        path: String,
        service: ServiceHub,
        whoAmI: Party,
        filename: String
    ): String {
        val attachmentHash = service.attachments.importAttachment(
            File(path).inputStream(),
            whoAmI.toString(),
            filename
        )

        return attachmentHash.toString();
    }

    @Suspendable
    fun attachmentToFile(input: InputStream, path: String) {
        File(path).outputStream().use { input.copyTo(it) }
    }

    @Suspendable
    fun divideKeys(
        serviceHub: ServiceHub,
        keys: Set<PublicKey>,
        ourIdentity: Party
    ): Pair<MutableList<PublicKey>, MutableList<PublicKey>> {
        val keysOnThisNode = mutableListOf<PublicKey>()
        val keysOnOtherNode = mutableListOf<PublicKey>()
        keys.forEach {
            if (serviceHub.identityService.partyFromKey(it) == ourIdentity) {
                keysOnThisNode.add(it)
            } else {
                keysOnOtherNode.add(it)
            }
        }
        return Pair(keysOnThisNode, keysOnOtherNode)
    }
}