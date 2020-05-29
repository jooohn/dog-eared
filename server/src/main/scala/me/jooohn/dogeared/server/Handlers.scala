package me.jooohn.dogeared.server

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}

class Handlers {

  def graphql(event: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    context.getLogger.log("test")
    println("test stdin")
    val response = new APIGatewayProxyResponseEvent()
    response.setStatusCode(200)
    response.withBody(event.getBody)
    response
  }

}
