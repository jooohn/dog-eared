package me.jooohn.dogeared

import ciris.ConfigValue
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

package object app {
  import ciris._

  lazy val secretsmanagerClient: SecretsManagerAsyncClient = SecretsManagerAsyncClient.builder().build()

  def envOrSecret(name: String): ConfigValue[String] =
    env(name).or(env(s"${name}_ARN").flatMap { arn =>
      secret(arn)
    })

  def secret(arn: String): ConfigValue[String] =
    ConfigValue.async { cb =>
      val request = GetSecretValueRequest
        .builder()
        .secretId(arn)
        .build()
      val response = secretsmanagerClient.getSecretValue(request)
      response.whenComplete((response, error) => {
        if (response != null) {
          cb(Right(ConfigValue.default(response.secretString())))
        } else {
          cb(Left(error))
        }
      })
    }

}
