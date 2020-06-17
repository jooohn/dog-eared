package me.jooohn.dogeared

import ciris.ConfigValue
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import software.amazon.awssdk.services.ssm.{SsmAsyncClient, SsmAsyncClientBuilder}

package object app {

  lazy val ssmClient: SsmAsyncClient = SsmAsyncClient.builder().build()

  implicit class ConfigValueOps(configValue: ConfigValue[String]) {

    def orSsmSecret(name: String): ConfigValue[String] = configValue.or {
      ConfigValue.async { cb =>
        val request = GetParameterRequest
          .builder()
          .name(name)
          .withDecryption(true)
          .build()
        val response = ssmClient.getParameter(request)
        response.whenComplete((parameter, error) => {
          if (parameter != null) {
            cb(Right(ConfigValue.default(parameter.parameter().value())))
          } else {
            cb(Left(error))
          }
        })
      }
    }

  }

}
