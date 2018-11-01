package io.shiftleft.queryprimitives.steps.types.structure

import gremlin.scala._
import gremlin.scala.dsl.Converter
import io.shiftleft.codepropertygraph.generated.{EdgeTypes, NodeKeys, NodeTypes}
import io.shiftleft.codepropertygraph.generated.nodes
import io.shiftleft.queryprimitives.steps.CpgSteps
import io.shiftleft.queryprimitives.steps.Implicits._
import io.shiftleft.queryprimitives.steps.types.expressions.generalizations.{DeclarationBase, Expression}
import io.shiftleft.queryprimitives.steps.types.propertyaccessors._
import shapeless.HList

/**
  * Formal method input parameter
  * */
class MethodParameter[Labels <: HList](raw: GremlinScala[Vertex])
    extends CpgSteps[nodes.MethodParameterIn, Labels](raw)
    with DeclarationBase[nodes.MethodParameterIn, Labels]
    with CodeAccessors[nodes.MethodParameterIn, Labels]
    with NameAccessors[nodes.MethodParameterIn, Labels]
    with OrderAccessors[nodes.MethodParameterIn, Labels]
    with LineNumberAccessors[nodes.MethodParameterIn, Labels]
    with EvalTypeAccessors[nodes.MethodParameterIn, Labels] {
  override val converter = Converter.forDomainNode[nodes.MethodParameterIn]

  /**
    * Traverse to all `num`th parameters
    * */
  def index(num: Int): MethodParameter[Labels] =
    order(num)

  /**
    * Traverse to all parameters with index greater or equal than `num`
    * */
  /* get all parameters from (and including)
   * method parameter indexes are  based, i.e. first parameter has index (that's how java2cpg generates it) */
  def indexFrom(num: Int): MethodParameter[Labels] =
    new MethodParameter[Labels](raw.has(NodeKeys.METHOD_PARAMETER_IN.ORDER, P.gte(num: Integer)))

  /**
    * Traverse to all parameters with index smaller or equal than `num`
    * */
  /* get all parameters up to (and including)
   * method parameter indexes are  based, i.e. first parameter has index  (that's how java2cpg generates it) */
  def indexTo(num: Int): MethodParameter[Labels] =
    new MethodParameter[Labels](raw.has(NodeKeys.METHOD_PARAMETER_IN.ORDER, P.lte(num: Integer)))

  /**
    * Traverse to method associated with this formal parameter
    * */
  def method: Method[Labels] =
    new Method[Labels](raw.in(EdgeTypes.AST))

  /**
    * Traverse to arguments (actual parameters) associated with this formal parameter
    * */
  def argument() =
    new Expression[Labels](raw.in(EdgeTypes.CALL_ARG))

  /**
    * Traverse to corresponding formal output parameter
    * */
  def asOutput: MethodParameterOut[Labels] =
    new MethodParameterOut[Labels](raw.out(EdgeTypes.PARAMETER_LINK))

  /**
    * Traverse to parameter type
    * */
  def typ: Type[Labels] =
    new Type(raw.out(EdgeTypes.EVAL_TYPE))

}
