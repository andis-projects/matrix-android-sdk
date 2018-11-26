/*
 * Copyright 2018 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.androidsdk.data.cryptostore.db

import android.util.Base64
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmObject
import org.matrix.androidsdk.util.CompatUtil
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream


/**
 * Compute a Hash of a String, using md5 algorithm
 */
fun String.hash() = try {
    val digest = MessageDigest.getInstance("md5")
    digest.update(toByteArray())
    val bytes = digest.digest()
    val sb = StringBuilder()
    for (i in bytes.indices) {
        sb.append(String.format("%02X", bytes[i]))
    }
    sb.toString().toLowerCase()
} catch (exc: Exception) {
    // Should not happen, but just in case
    hashCode().toString()
}

fun <T> doWithRealm(realmConfiguration: RealmConfiguration, action: (Realm) -> T): T {
    val realm = Realm.getInstance(realmConfiguration)
    val result = action.invoke(realm)
    realm.close()
    return result
}

/**
 * Open realm, to the query, copy from realm and close realm
 */
fun <T : RealmObject> doRealmQueryAndCopy(realmConfiguration: RealmConfiguration, action: (Realm) -> T?): T? {
    val realm = Realm.getInstance(realmConfiguration)
    val result = action.invoke(realm)
    val copiedResult = result?.let { realm.copyFromRealm(result) }
    realm.close()
    return copiedResult
}

fun <T : RealmObject> doRealmQueryAndCopyList(realmConfiguration: RealmConfiguration, action: (Realm) -> Iterable<T>): Iterable<T> {
    val realm = Realm.getInstance(realmConfiguration)
    val result = action.invoke(realm)
    val copiedResult = realm.copyFromRealm(result)
    realm.close()
    return copiedResult
}

fun doRealmTransaction(realmConfiguration: RealmConfiguration, action: (Realm) -> Unit) {
    val realm = Realm.getInstance(realmConfiguration)
    realm.executeTransaction { action.invoke(it) }
    realm.close()
}

fun serializeForRealm(o: Any?): String? {
    if (o == null) {
        return null
    }

    val baos = ByteArrayOutputStream()
    val gzis = CompatUtil.createGzipOutputStream(baos)
    val out = ObjectOutputStream(gzis)

    out.writeObject(o)
    out.close()

    return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
}

fun <T> deserializeFromRealm(string: String?): T? {
    if (string == null) {
        return null
    }

    val decodedB64 = Base64.decode(string.toByteArray(), Base64.DEFAULT)

    val bais = ByteArrayInputStream(decodedB64)
    val gzis = GZIPInputStream(bais)
    val ois = ObjectInputStream(gzis)

    val result = ois.readObject() as T

    ois.close()

    return result
}
