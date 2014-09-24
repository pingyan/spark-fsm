package de.kp.spark.fsm.actor
/* Copyright (c) 2014 Dr. Krusche & Partner PartG
* 
* This file is part of the Spark-FSM project
* (https://github.com/skrusche63/spark-fsm).
* 
* Spark-FSM is free software: you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation, either version 3 of the License, or (at your option) any later
* version.
* 
* Spark-FSM is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with
* Spark-FSM. 
* 
* If not, see <http://www.gnu.org/licenses/>.
*/

import akka.actor.{Actor,ActorLogging}

import de.kp.spark.fsm.model._
import de.kp.spark.fsm.util.{PatternCache,RuleCache}

class FSMQuestor extends Actor with ActorLogging {

  implicit val ec = context.dispatcher
  
  def receive = {

    case req:ServiceRequest => {
      
      val origin = sender    
      val uid = req.data("uid")
      
      req.task match {
        
        case "predict" => {

          val resp = if (RuleCache.exists(uid) == false) {           
            failure(req,Messages.RULES_DO_NOT_EXIST(uid))
            
          } else {    

            val antecedent = req.data.getOrElse("antecedent", null) 
            val consequent = req.data.getOrElse("consequent", null)            

            if (antecedent == null && consequent == null) {
               failure(req,Messages.NO_ANTECEDENTS_OR_CONSEQUENTS_PROVIDED(uid))
             
             } else {

               val rules = (if (antecedent != null) {
                 val items = antecedent.split(",").map(_.toInt).toList
                 RuleCache.rulesByAntecedent(uid,items)
               
               } else {
                 val items = consequent.split(",").map(_.toInt).toList
                 RuleCache.rulesByConsequent(uid,items)
                 
               }).map(rule => rule.toJSON).mkString(",")
               
               val data = Map("uid" -> uid, "rules" -> rules)
               new ServiceResponse(req.service,req.task,data,FSMStatus.SUCCESS)
             
             }
            
          }
           
          origin ! FSMModel.serializeResponse(resp)
        }

        case "patterns" => {

          val resp = if (PatternCache.exists(uid) == false) {           
            failure(req,Messages.PATTERNS_DO_NOT_EXIST(uid))
            
          } else {            
            val patterns = PatternCache.patterns(uid).map(pattern => pattern.toJSON).mkString(",")
               
            val data = Map("uid" -> uid, "patterns" -> patterns)
            new ServiceResponse(req.service,req.task,data,FSMStatus.SUCCESS)
            
          }
           
          origin ! FSMModel.serializeResponse(resp)
          
        }
        
        case "rules" => {
          
          val resp = if (RuleCache.exists(uid) == false) {           
            failure(req,Messages.RULES_DO_NOT_EXIST(uid))
            
          } else {            
            
            val rules = RuleCache.rules(uid).map(rule => rule.toJSON).mkString(",")
               
            val data = Map("uid" -> uid, "rules" -> rules)
            new ServiceResponse(req.service,req.task,data,FSMStatus.SUCCESS)
            
          }
           
          origin ! FSMModel.serializeResponse(resp)
           
        }
        
      }
      
    }
  
  }

  private def failure(req:ServiceRequest,message:String):ServiceResponse = {
    
    val data = Map("uid" -> req.data("uid"), "message" -> message)
    new ServiceResponse(req.service,req.task,data,FSMStatus.FAILURE)	
    
  }
  
}