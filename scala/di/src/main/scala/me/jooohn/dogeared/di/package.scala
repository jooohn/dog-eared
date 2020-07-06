package me.jooohn.dogeared
import scala.reflect.runtime.universe.TypeTag

package object di {

  type Memo = Map[TypeTag[_], _]

}
