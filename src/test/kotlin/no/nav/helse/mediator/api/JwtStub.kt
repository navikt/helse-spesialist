package no.nav.helse.mediator.api

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.mockk.every
import io.mockk.mockk
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

internal class JwtStub {
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048, SecureRandom())
    }.generateKeyPair()
    private val jwkAlgorithm = Algorithm.RSA256(keyPair.public as RSAPublicKey, keyPair.private as RSAPrivateKey)

    internal fun getJwkProviderMock(): JwkProvider {
        val jwk = mockk<Jwk>().apply {
            every { algorithm } returns jwkAlgorithm.name
            every { publicKey } returns keyPair.public
        }
        return mockk<JwkProvider>().apply {
            every { this@apply.get(any()) } returns jwk
        }
    }

    internal fun getToken(
        groups: Collection<String>,
        oid: String,
        epostadresse: String,
        clientId: String,
        issuer: String,
        navn: String = "navn",
    navIdent: String = "X999999"
    ) = JWT.create()
        .withArrayClaim("groups", groups.toTypedArray())
        .withClaim("oid", oid)
        .withClaim("preferred_username", epostadresse)
        .withClaim("name", navn)
        .withClaim("NAVident", navIdent)
        .withAudience(clientId)
        .withKeyId("randomString")
        .withIssuer(issuer)
        .sign(jwkAlgorithm)
}
