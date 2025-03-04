package scala.generator

import scala.annotation.nowarn

import scala.models.{Attributes, ResponseConfig}

sealed trait ScalaClientMethodConfig {

  /**
    * Namespace in which the client is defined
    */
  def namespace: String

  /**
   * Configuration related to all generator attributes
   */
  def attributes: Attributes

  /**
    * Base URL for the service if provided
    */
  def baseUrl: Option[String]

  /**
    * The code to generate to encode a variable into a path.
    */
  def pathEncode(value: String): String

  /**
    * The name of the method on the response providing the status code.
    */
  def responseStatusMethod: String

  /**
    * The name of the method on the response providing the request URI.
    */
  def requestUriMethod: Option[String]

  /**
    * The name of the method on the response providing the body.
    */
  def responseBodyMethod: String

  /**
    * The class name for the Response object.
    */
  def responseClass: String

  /** Whether or not play.api.libs.ws.WS exists as a static object, or if
    * play.api.libs.ws.WSClient is expected to be passed in.
    */
  def expectsInjectedWsClient: Boolean

  /**
    * Extra arguments that we need to provide to the Client, like e.g.
    * our own async http client
    */
  def extraClientCtorArgs: Option[String]

  /**
    * Extra methods to add to the Client object
    */
  def extraClientObjectMethods: Option[String]

  /**
    * true if the native libraries can serialized UUID. False
    * otherwise. If false, we map the UUIDs to strings in the
    * generated code.
    */
  def canSerializeUuid: Boolean

  /**
    * If client methods require a second (implicit) parameter list,
    * e.g. to pass in an ExecutionContext, this can be specified here.
    */
  def implicitArgs: Option[String]

  def asyncType: String

  def asyncSuccess: String

  def formatBaseUrl(url: Option[String]): String = s"val baseUrl: String" + url.fold("")(u => s" = ${ScalaUtil.wrapInQuotes(u)}")

  /**
    * Given a response and a class name, returns code to create an
    * instance of the specified class.
    */
  def toJson(responseName: String, className: String): String = {
    toClientMethodCall("parseJson", responseName, className)
  }

  private[this] def toClientMethodCall(methodName: String, responseName: String, className: String): String = {
    s"""_root_.$namespace.Client.$methodName("$className", $responseName, _.validate[$className])"""
  }

  final def buildResponse(responseName: String, className: String): String = {
    responseEnvelopeClassName match {
      case None => toJson(responseName, className)
      case Some(_) => toClientMethodCall("buildResponse", responseName, className)
    }
  }

  def asyncTypeParam(@nowarn constraint: Option[String] = None): Option[String] = None

  def wrappedAsyncType(@nowarn instance: String = ""): Option[String] = None

  def asyncSuccessInvoke: String = wrappedAsyncType("Sync").getOrElse(asyncType) + "." + asyncSuccess

  final def responseEnvelopeClassName: Option[String] = attributes.response match {
    case ResponseConfig.Envelope => Some("Response")
    case ResponseConfig.Standard => None
  }
}

object ScalaClientMethodConfigs {

  trait Play extends ScalaClientMethodConfig {
    override def toJson(responseName: String, className: String): String = {
      if (className == "_root_.play.api.libs.json.JsValue") {
        s"$responseName.json" // use the built-in response.json method
      } else {
        super.toJson(responseName, className)
      }
    }
    override def pathEncode(value: String) = s"""play.utils.UriEncoding.encodePathSegment($value, "UTF-8")"""
    override val responseStatusMethod = "status"
    override val responseBodyMethod = "body"
    override val expectsInjectedWsClient = false
    override val extraClientCtorArgs: Option[String] = None
    override val extraClientObjectMethods: Option[String] = None
    override val implicitArgs = Some("(implicit ec: scala.concurrent.ExecutionContext)")
    override val asyncType: String = "scala.concurrent.Future"
    override val asyncSuccess: String = "successful"
  }

  case class Play22(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Play {
    override val responseClass = "play.api.libs.ws.Response"
    override val requestUriMethod = Some("ahcResponse.getUri")
    override val expectsInjectedWsClient = false
    override val canSerializeUuid = false
  }

  case class Play23(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Play {
    override val responseClass = "play.api.libs.ws.WSResponse"
    override val requestUriMethod: Option[String] = None
    override val expectsInjectedWsClient = false
    override val canSerializeUuid = true
  }

  case class Play24(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Play {
    override val responseClass = "play.api.libs.ws.WSResponse"
    override val requestUriMethod: Option[String] = None
    override val expectsInjectedWsClient = false
    override val canSerializeUuid = true
  }

  case class Play25(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Play {
    override val responseClass = "play.api.libs.ws.WSResponse"
    override val requestUriMethod: Option[String] = None
    override val expectsInjectedWsClient = true
    override val canSerializeUuid = true
  }

  case class Play26(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Play {
    override val responseClass = "play.api.libs.ws.WSResponse"
    override val requestUriMethod: Option[String] = None
    override val expectsInjectedWsClient = true
    override val canSerializeUuid = true
  }

  case class Play26Envelope(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Play {
    override val responseClass = "play.api.libs.ws.WSResponse"
    override val requestUriMethod: Option[String] = None
    override val expectsInjectedWsClient = true
    override val canSerializeUuid = true
  }

  case class Play27(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Play {
    override val responseClass = "play.api.libs.ws.WSResponse"
    override val requestUriMethod: Option[String] = None
    override val expectsInjectedWsClient = true
    override val canSerializeUuid = true
  }

  case class Play28(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Play {
    override val responseClass = "play.api.libs.ws.WSResponse"
    override val requestUriMethod: Option[String] = None
    override val expectsInjectedWsClient = true
    override val canSerializeUuid = true
  }

  trait Ning extends ScalaClientMethodConfig {
    override def pathEncode(value: String) = s"""_root_.$namespace.PathSegment.encode($value, "UTF-8")"""
    override val responseStatusMethod = "getStatusCode"
    override val responseBodyMethod = """getResponseBody("UTF-8")"""
    override val responseClass = "_root_.com.ning.http.client.Response"
    override val extraClientCtorArgs = Some(",\n  asyncHttpClient: AsyncHttpClient = Client.defaultAsyncHttpClient")
    override val extraClientObjectMethods = Some("""
private lazy val defaultAsyncHttpClient = {
  new AsyncHttpClient(
    new AsyncHttpClientConfig.Builder()
      .setExecutorService(java.util.concurrent.Executors.newCachedThreadPool())
      .build()
  )
}
""")
    override val canSerializeUuid = true
    override val implicitArgs = Some("(implicit ec: scala.concurrent.ExecutionContext)")
    override val asyncType: String = "scala.concurrent.Future"
    override val asyncSuccess: String = "successful"
    override val requestUriMethod = Some("getUri.toJavaNetURI")
    override val expectsInjectedWsClient = false

    def addQueryParamMethod: String = "addQueryParam"
    def ningPackage: String = "com.ning.http.client"
    def additionalImports: String = ", AsyncHttpClientConfig"
    def realmBuilder(username: String, password: String) =
      s"""|new Realm.RealmBuilder()
          |  .setPrincipal($username)
          |  .setPassword($password)""".stripMargin
  }

  case class Ning18(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Ning {
    override def addQueryParamMethod: String = "addQueryParameter"
    override val requestUriMethod = Some("getUri")
  }

  case class Ning19(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Ning

  case class AsyncHttpClient(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Ning {
    override def ningPackage: String = "org.asynchttpclient"
    override val responseBodyMethod = """getResponseBody(java.nio.charset.Charset.forName("UTF-8"))"""
    override val responseClass = "_root_.org.asynchttpclient.Response"
    override def additionalImports: String = ", DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig"
    override def realmBuilder(username: String, password: String) = s"new Realm.Builder($username, $password)"
    override val extraClientObjectMethods = Some("""
private lazy val defaultAsyncHttpClient = {
  new DefaultAsyncHttpClient(
    new DefaultAsyncHttpClientConfig.Builder().build()
  )
}
""")
  }


  sealed trait Http4s extends ScalaClientMethodConfig {
    override def pathEncode(value: String): String = value
    override val responseStatusMethod = "status.code"
    override val responseBodyMethod = """body"""
    override val responseClass = "org.http4s.Response"
    override def extraClientCtorArgs: Option[String] = Some(",\n  asyncHttpClient: org.http4s.client.Client = Client.defaultAsyncHttpClient")
    override val extraClientObjectMethods = Some("""
implicit def circeJsonDecoder[A](implicit decoder: io.circe.Decoder[A]) = org.http4s.circe.jsonOf[A]

private lazy val defaultAsyncHttpClient = PooledHttp1Client()
""")
    override val canSerializeUuid = true
    override val implicitArgs: Option[String] = None
    override def asyncSuccess: String = "now"
    override val requestUriMethod: Option[String] = None //Some("getUri.toJavaNetURI")
    override val expectsInjectedWsClient = false
    override def formatBaseUrl(url: Option[String]): String = s"val baseUrl: org.http4s.Uri" + url.fold("")(u => s" = org.http4s.Uri.unsafeFromString(${ScalaUtil.wrapInQuotes(u)})")
    override def toJson(responseName: String, className: String): String = {
      s"""_root_.${namespace}.Client.parseJson[${if(asyncType == "F")"F, " else ""}$className]("$className", $responseName)"""
    }
    def leftType: String
    def rightType: String
    def monadTransformerInvoke: String
    def asyncFailure: String
    def requestClass: String = "org.http4s.Request"
    def headerRawClass: String = "org.http4s.Header"
    def requestType:  String = s"$asyncType[$requestClass]"
    def messageClass: String = "org.http4s.Message"
    def httpServiceClass: String = "org.http4s.HttpService"
    def generateDecodeResult(datatypeName: String): String = s"org.http4s.DecodeResult[$datatypeName]"
    def generateCirceJsonOf(datatypeName: String): String = s"org.http4s.circe.jsonOf[$datatypeName]"
    def generateCirceJsonEncoderOf(datatypeName: String): String = s"org.http4s.circe.jsonEncoderOf[$datatypeName]"
    def serverImports: String = "\nimport org.http4s.dsl._\n"
    def routeKind: String = "trait"
    def matcherKind: String = "object"
    def asyncTypeImport: String = ""
    def routeExtends: Option[String] = None
    def matchersExtends: Option[String] = None
    def clientImports: String = """import org.http4s.client.blaze._
                                  |import io.circe.syntax._""".stripMargin
    def closeClient: Option[String] = Some("""
                                             |def closeAsyncHttpClient(): Unit = {
                                             |  asyncHttpClient.shutdownNow()
                                             |}""".stripMargin)

    def reqAndMaybeAuthAndBody: String = s"""val reqAndMaybeAuthAndBody =
                                            |  if (formBody.nonEmpty) formBody.fold($asyncSuccessInvoke(reqAndMaybeAuth))(reqAndMaybeAuth.withBody)
                                            |  else body.fold($asyncSuccessInvoke(reqAndMaybeAuth))(reqAndMaybeAuth.withBody)""".stripMargin

    def matchersImport: String
    def httpClient: String
    def applicationJsonMediaType: String
    def headerConstructor(headerName: String, headerValue: String) = s"org.http4s.Header($headerName, $headerValue)"
    def headerOptSelection(requestVariableName:String, headerName: String) = s"${requestVariableName}.headers.get($headerName)"
  }

  case class Http4s015(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Http4s {
    override val asyncType = "scalaz.concurrent.Task"
    override val leftType = "scalaz.-\\/"
    override val rightType = "scalaz.\\/-"
    override val monadTransformerInvoke = "run"
    override def asyncFailure: String = "fail"
    override val matchersImport: String = "\n  import Matchers._\n"
    override val httpClient: String = "asyncHttpClient"
    override val applicationJsonMediaType: String = "_root_.org.http4s.MediaType.`application/json`"
  }

  case class Http4s017(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Http4s {
    override val asyncType = "fs2.Task"
    override val leftType = "Left"
    override val rightType = "Right"
    override val monadTransformerInvoke = "value"
    override def asyncFailure: String = "fail"

    override val matchersImport: String = "\n  import Matchers._\n"
    override val httpClient: String = "asyncHttpClient"
    override val applicationJsonMediaType: String = "_root_.org.http4s.MediaType.`application/json`"
  }

  case class Http4s018(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Http4s {
    override val asyncType = "F"
    override def asyncTypeParam(constraint: Option[String] = None) = Some(s"$asyncType[_]${constraint.map(c => s": $c").getOrElse("")}")
    override val leftType = "Left"
    override val rightType = "Right"
    override val monadTransformerInvoke = "value"
    override val responseClass = s"org.http4s.Response[$asyncType]"
    override val extraClientCtorArgs: Option[String] = Some(s",\n  httpClient: org.http4s.client.Client[$asyncType]")
    override val extraClientObjectMethods = Some(
      s"""implicit def circeJsonDecoder[${asyncTypeParam(Some("Sync")).map(_+", ").getOrElse("")}A](implicit decoder: io.circe.Decoder[A]) = org.http4s.circe.jsonOf[$asyncType, A]"""
    )
    override val asyncSuccess: String = "pure"
    override def asyncFailure: String = "raiseError"
    override def requestClass: String = s"org.http4s.Request[$asyncType]"
    override def generateDecodeResult(datatypeName: String): String = s"org.http4s.DecodeResult[$asyncType, $datatypeName]"
    override def messageClass: String = s"org.http4s.Message[$asyncType]"
    override def httpServiceClass: String = s"org.http4s.HttpService[$asyncType]"
    override def generateCirceJsonOf(datatypeName: String): String = s"org.http4s.circe.jsonOf[$asyncType, $datatypeName]"
    override def generateCirceJsonEncoderOf(datatypeName: String): String = s"org.http4s.circe.jsonEncoderOf[$asyncType, $datatypeName]"

    override def serverImports: String =
      s"""
         |import org.http4s.circe.decodeUri
         |import org.http4s.circe.encodeUri
         |import org.http4s.dsl.{io => _, _}
         |import cats.effect._
         |import cats.implicits._
         |import scala.language.higherKinds""".stripMargin


    override val routeKind = "trait"
    override def wrappedAsyncType(instance: String = "") = Some(s"$instance[$asyncType]")
    override val routeExtends: Option[String] = Some(s" extends Matchers[$asyncType]")
    override val matchersExtends = Some(s" extends Http4sDsl[$asyncType]")
    override val clientImports: String = """import cats.effect._
                                           |import cats.implicits._
                                           |import io.circe.syntax._
                                           |import scala.language.higherKinds""".stripMargin

    override val closeClient = None

    override val matcherKind: String = "trait"
    override val matchersImport: String = ""
    override val httpClient: String = "httpClient"

    override val asyncTypeImport: String = "import cats.effect._"
    override val applicationJsonMediaType: String = "_root_.org.http4s.MediaType.`application/json`"
  }

  sealed trait Http4s02xSeries extends Http4s {
    override val asyncType = "F"
    override def asyncTypeParam(constraint: Option[String] = None) = Some(s"$asyncType[_]${constraint.map(c => s": $c").getOrElse("")}")
    override val leftType = "Left"
    override val rightType = "Right"
    override val monadTransformerInvoke = "value"
    override val responseClass = s"org.http4s.Response[$asyncType]"
    override val extraClientCtorArgs: Option[String] = Some(s",\n  httpClient: org.http4s.client.Client[$asyncType]")
    override val extraClientObjectMethods = Some(
      s"""implicit def circeJsonDecoder[${asyncTypeParam(Some("Sync")).map(_+", ").getOrElse("")}A](implicit decoder: io.circe.Decoder[A]) = org.http4s.circe.jsonOf[$asyncType, A]"""
    )
    override val asyncSuccess: String = "pure"
    override def asyncFailure: String = "raiseError"
    override def requestClass: String = s"org.http4s.Request[$asyncType]"
    override def requestType:  String = requestClass
    override def generateDecodeResult(datatypeName: String): String = s"org.http4s.DecodeResult[$asyncType, $datatypeName]"
    override def messageClass: String = s"org.http4s.Message[$asyncType]"
    override def httpServiceClass: String = s"org.http4s.HttpRoutes.of[$asyncType]"
    override def generateCirceJsonOf(datatypeName: String): String = s"org.http4s.circe.jsonOf[$asyncType, $datatypeName]"
    override def generateCirceJsonEncoderOf(datatypeName: String): String = s"org.http4s.circe.jsonEncoderOf[$asyncType, $datatypeName]"

    override def serverImports: String =
      s"""
         |import org.http4s.circe.decodeUri
         |import org.http4s.circe.encodeUri
         |import org.http4s.dsl.{io => _, _}
         |import org.http4s.implicits._
         |import cats.effect._
         |import cats.implicits._""".stripMargin


    override val routeKind = "trait"
    override def wrappedAsyncType(instance: String = "") = Some(s"$instance[$asyncType]")
    override val routeExtends: Option[String] = Some(s" extends Matchers[$asyncType]")
    override val matchersExtends = Some(s" extends Http4sDsl[$asyncType]")
    override val clientImports: String = """import cats.effect._
                                           |import cats.implicits._
                                           |import io.circe.syntax._""".stripMargin

    override val closeClient = None

    override val matcherKind: String = "trait"
    override val matchersImport: String = ""
    override val httpClient: String = "httpClient"

    override val asyncTypeImport: String = "import cats.effect._"

    override def reqAndMaybeAuthAndBody: String = """val reqAndMaybeAuthAndBody =
                                                    |  if (formBody.nonEmpty) formBody.fold(reqAndMaybeAuth)(reqAndMaybeAuth.withEntity)
                                                    |  else body.fold(reqAndMaybeAuth)(reqAndMaybeAuth.withEntity)""".stripMargin

    override val applicationJsonMediaType: String = "_root_.org.http4s.MediaType.application.json"
  }

  case class Http4s020(namespace: String, attributes: Attributes, baseUrl: Option[String]) extends Http4s02xSeries

  case class Http4s022(override val namespace: String, override val attributes: Attributes, override val baseUrl: Option[String]) extends Http4s02xSeries {

    override def headerConstructor(headerName: String, headerValue: String): String =
      s"""org.http4s.Header.Raw(org.typelevel.ci.CIString($headerName), $headerValue)"""

    override def headerOptSelection(requestVariableName:String, headerName: String) =
      s"${requestVariableName}.headers.get($headerName).map(_.head)"

    override def headerRawClass: String = "org.http4s.Header.ToRaw"

  }
}
