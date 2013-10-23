// 
// 
// Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.  
// Contributors retain copyright to elements licensed under a Contributor Agreement.
// Licensed to the User under the LGPL license.
// 
// $$
//////////////////////////////////////////////////////////////////////////////

// SYSTEM INCLUDES
#include <assert.h>
#include <stdlib.h>

// APPLICATION INCLUDES
#include "SipXProxyCstaObserver.h"
#include <net/SipUserAgent.h>
#include <os/OsDateTime.h>
#include "os/OsEventMsg.h"
#include <os/OsLogger.h>
#include <os/OsConnectionSocket.h>
#include <os/OsServerSocket.h>
#include <os/OsServerTask.h>
#include <os/OsTask.h>
#include <os/OsLock.h>
//#include <net/XmlRpcRequest.h>
//#include <net/XmlRpcResponse.h>
//#include <net/XmlRpcDispatch.h>

//#define TEST_PRINT 1
#define LOG_DEBUG 1
#define FIELD_SEP ", "


// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS
MySockServ::MySockServ(int port)
{
  m_Server = new OsServerSocket(3,port);
  m_SockConn= NULL;
};

MySockServ::~MySockServ()
{
  if(m_SockConn!=NULL)
    {
      m_SockConn->close();
    }
  m_Server->close();
  delete m_SockConn;
  delete m_Server;
};

void MySockServ::write(const char *buff, int len)
{ 
  if(m_SockConn != NULL){
    if(m_SockConn->isReadyToWrite())
      {
	OsLock lock(mDataGuard);
	int ret = m_SockConn->write(buff,len);
	if (-1 == ret)
	  {
	    m_SockConn->close();
	    m_SockConn = NULL;
	    //OsLogger::add(FAC_SIP, PRI_ERR, "%d -----------------> %d", len, ret);
	  }
      }
  }
};

int MySockServ::run(void*)
{
  OsSocket* requestSocket = NULL;
  //printf("doh\n");
  if (!m_Server->isOk())
    {
      printf("troubles with port\n");
      //OsLogger::add( FAC_SIP, PRI_ERR, "MySockServ: port not ok" );
    }
  while(!isShuttingDown() && m_Server->isOk())
    {
      requestSocket = m_Server->accept();
      if(requestSocket)
	{
	  if(m_SockConn == NULL){
	    m_SockConn = requestSocket;
	  }
	  else{
	    OsLock lock(mDataGuard);
	    // don't use write to use a simplified locking method
	    m_SockConn->write("\n",1); // try if socket is still active //
	    
	    if(!m_SockConn->isReadyToWrite())
	      {
		m_SockConn->close();
		delete m_SockConn;
		m_SockConn = requestSocket;
	      }
	    else
	      {
		requestSocket->close();
		delete requestSocket;
	      }
	  }
	}
    }
  return 1;
};

// STATIC VARIABLE INITIALIZATIONS
/* //////////////////////////// PUBLIC //////////////////////////////////// */
/* ============================ CREATORS ================================== */
// Constructor

SipXProxyCstaObserver::SipXProxyCstaObserver(SipUserAgent&        sipUserAgent,
					     int port):
   OsServerTask("SipXProxyCstaObserver-%d", NULL, 2000),
   mpSipUserAgent(&sipUserAgent)
   //mpServer(new OsServerSocket(50, port))
{
   UtlString event;
   // set up periodic timer to flush log file

  // Register to get incoming requests
   sipUserAgent.addMessageObserver(*getMessageQueue(),
                                   SIP_BYE_METHOD,
                                   TRUE, // Requests,
                                   FALSE, //Responses,
                                   TRUE, //Incoming,
                                   FALSE, //OutGoing,
                                   "", //eventName,
                                   NULL, // any session
                                   NULL // no observerData
                                   );
   sipUserAgent.addMessageObserver(*getMessageQueue(),
                                   SIP_REGISTER_METHOD,
                                   FALSE, // Requests,
                                   TRUE, //Responses,
				   TRUE, //Incoming,
                                   FALSE, //OutGoing,
                                   "", //eventName,
                                   NULL, // any session
                                   NULL // no observerData
                                   );
   sipUserAgent.addMessageObserver(*getMessageQueue(),
                                   SIP_INVITE_METHOD,
                                   TRUE, // Requests,
                                   TRUE, //Responses,
                                   TRUE, //Incoming,
                                   FALSE, //OutGoing,
                                   "", //eventName,
                                   NULL, // any session
                                   NULL // no observerData
                                   );
   sipUserAgent.addMessageObserver(*getMessageQueue(),
                                   SIP_REFER_METHOD,
                                   TRUE, // Requests,
                                   FALSE, //Responses,
                                   TRUE, //Incoming,
                                   FALSE, //OutGoing,
                                   "", //eventName,
                                   NULL, // any session
                                   NULL // no observerData
                                   );
   
   mpServ = new MySockServ(port);
   mpServ->start();
}

// Destructor
SipXProxyCstaObserver::~SipXProxyCstaObserver()
{
  //mpServer->close();
  //delete mpServer;
}

/* ============================ MANIPULATORS ============================== */

UtlBoolean SipXProxyCstaObserver::handleMessage(OsMsg& eventMessage)
{
   int msgType = eventMessage.getMsgType();
   switch (msgType)
   {
   case OsMsg::OS_EVENT:
      switch (eventMessage.getMsgSubType())
      {
      case OsEventMsg::NOTIFY:
         break;
      }
      break ;
      
   case OsMsg::PHONE_APP:
   {
      SipMessage* sipMsg;

      if(SipMessageEvent::TRANSPORT_ERROR == ((SipMessageEvent&)eventMessage).getMessageStatus())
      {
	Os::Logger::instance().log(FAC_SIP, PRI_ERR,
                       "SipXProxyCstaObserver::handleMessage transport error");
      }
      else if((sipMsg = (SipMessage*)((SipMessageEvent&)eventMessage).getMessage()))
      {
         UtlString method;
         int       rspStatus = 0;
         UtlString rspText;
         UtlString contact;
         UtlString toTag;
	 UtlString xChan;
         
         enum 
            {
               UnInteresting,
               aCallRequest,
               aRegisterRequest,
	       aCallProceeding,
               aCallSetup,
	       aCallModification,
               aCallFailure,
               aCallEnd,
               aCallTransfer
            } thisMsgIs = UnInteresting;
         
         Url toUrl;
         sipMsg->getToUrl(toUrl);
	 xChan = sipMsg->getHeaderValue(0,"X-Channel"); // oh!!
         // explicitly, an INVITE Request
         toUrl.getFieldParameter("tag", toTag);
	 // try to get line field
	 
	 sipMsg->getContactEntry(0, &contact);
         if (!sipMsg->isResponse())
         {
            // sipMsg is a Request
            sipMsg->getRequestMethod(&method);

            if (0==method.compareTo(SIP_INVITE_METHOD, UtlString::ignoreCase))
            {
               if (toTag.isNull())
               {
                  thisMsgIs = aCallRequest;
               }
	       else
		 {
		   thisMsgIs = aCallModification;
		 }

            }
            else if (0==method.compareTo(SIP_REFER_METHOD, UtlString::ignoreCase))
            {
               thisMsgIs = aCallTransfer;
               sipMsg->getContactEntry(0, &contact);               
            }
            else if (0==method.compareTo(SIP_BYE_METHOD, UtlString::ignoreCase))
            {
               thisMsgIs = aCallEnd; // no additional information needed
            }
            else
            {
               // other request methods are not interesting
            }
         }
         else // this is a response
         {
            int seq;
            if (sipMsg->getCSeqField(&seq, &method)) // get the method out of cseq field
            {
               if (0==method.compareTo(SIP_INVITE_METHOD, UtlString::ignoreCase))
               {
                  // Responses to INVITES are handled differently based on whether
                  // or not the INVITE is dialog-forming.  If dialog-forming,
                  // any final response above 400 is considered a failure for CDR
                  // purposes.  If not dialog-forming, then any final response above 400
                  // except 401 Unauthorized, 407 Proxy Authentication Required and 
                  // 408 Request Timeout

                  rspStatus = sipMsg->getResponseStatusCode();
                  if (   (rspStatus >= SIP_4XX_CLASS_CODE) // any failure
                      // except for these three on non-dialog-froming INVITES
                      && ( toTag.isNull() ||
                         ! (   (rspStatus == HTTP_UNAUTHORIZED_CODE)
                            || (rspStatus == HTTP_PROXY_UNAUTHORIZED_CODE)
                            || (rspStatus == SIP_REQUEST_TIMEOUT_CODE)
                            )
                          )
                      )
                  {
                     // a final failure - this is a CallFailure
                     thisMsgIs = aCallFailure;
                     sipMsg->getResponseStatusText(&rspText);
                  }
                  else if (   ( rspStatus >= SIP_2XX_CLASS_CODE )
                           && ( rspStatus <  SIP_3XX_CLASS_CODE )
                           )
                  {
                     thisMsgIs = aCallSetup;
                     sipMsg->getContactEntry(0, &contact);
                  }
		  else 
		  {
		      thisMsgIs = aCallProceeding;
		      sipMsg->getContactEntry(0, &contact);
		  }
               }
	       else if (0==method.compareTo(SIP_REGISTER_METHOD, UtlString::ignoreCase))
		 {
		   rspStatus = sipMsg->getResponseStatusCode();
		   if (( rspStatus >= SIP_2XX_CLASS_CODE ) && ( rspStatus <  SIP_3XX_CLASS_CODE ))
		     {
		       thisMsgIs = aRegisterRequest; // no additional information needed
		     }
		 }
               else
		 {
		   // responses to non-INVITES are not interesting
		 }
            }
            else
	      {
		Os::Logger::instance().log(FAC_SIP, PRI_ERR, "SipXProxyCstaObserver - no Cseq in response");
	      }
         }

#        ifdef LOG_DEBUG
         Os::Logger::instance().log(FAC_SIP, PRI_INFO, "SipXProxyCstaObserver message is %s",
                       (  thisMsgIs == UnInteresting ? "UnInteresting"
                        : thisMsgIs == aCallEnd      ? "a Call End"
                        : thisMsgIs == aCallFailure  ? "a Call Failure"
                        : thisMsgIs == aCallRequest  ? "a call Request"      
                        : thisMsgIs == aCallSetup    ? "a Call Setup"
                        : thisMsgIs == aCallTransfer ? "a Call Transfer"
                        : "BROKEN"
                        )); 
#        endif

         if (thisMsgIs != UnInteresting)
         {            
            OsTime timeNow;
            OsDateTime::getCurTime(timeNow);
	    //const SdpBody *sdpBody;
	    //int mediaPort;
            UtlString callId;
            sipMsg->getCallIdField(&callId);
         
            Url toUrl;
            sipMsg->getToUrl(toUrl);
            UtlString toTag;
            toUrl.getFieldParameter("tag", toTag);

            Url fromUrl;
            sipMsg->getFromUrl(fromUrl);
            UtlString fromTag;
            fromUrl.getFieldParameter("tag", fromTag);

            // collect the To and From
            UtlString toField;
            sipMsg->getToField(&toField);
            
            UtlString fromField;
            sipMsg->getFromField(&fromField);

            UtlString referTo;
            UtlString referredBy;
            UtlString requestUri;
            sipMsg->getReferToField(referTo);
            sipMsg->getReferredByField(referredBy);   
            sipMsg->getRequestUri(&requestUri);
            
            UtlString responseMethod;
	    UtlString msg;
            int cseqNumber;
            sipMsg->getCSeqField(&cseqNumber, &responseMethod);
	    
	    //XmlRpcRequest request(*rUrl,"proxy_event");
	    //UtlInt iResponse(sipMsg->isResponse()? -1 : 1);
	    UtlInt iCseq(7);
	    //UtlString *TmpInt;
	    
	    int ExpiresTime;
	    sipMsg->getExpiresField(&ExpiresTime);
	    UtlString temp_expire;
	    temp_expire.appendNumber(ExpiresTime);
	    
	    msg += (sipMsg->isResponse()? "-1" : "1");
	    msg += FIELD_SEP;
	    msg += method;
	    msg += FIELD_SEP;
	    msg.appendNumber(rspStatus);
	    msg += FIELD_SEP;
	    msg += callId;
	    msg += FIELD_SEP;
	    msg += fromField;
	    msg += FIELD_SEP;
	    msg += toField;
	    msg += FIELD_SEP;
	    msg += ((ExpiresTime == -1) ? xChan : temp_expire);
	    msg += FIELD_SEP;
	    msg += referTo;
	    msg += FIELD_SEP;
	    msg += contact;
	    msg += FIELD_SEP;
	    msg.appendNumber(cseqNumber);
	    msg += FIELD_SEP;
	    msg += referredBy;
	    msg += FIELD_SEP;
	    msg += requestUri;
	    msg += FIELD_SEP;
	    msg+="\n";
	    mpServ->write((const char *)msg, (int)msg.length());
         }
      }
      else
      {
	Os::Logger::instance().log(FAC_SIP, PRI_INFO, "SipXProxyCstaObserver getMessage returned NULL");
      }
   }
   break;
   
   default:
   {
      Os::Logger::instance().log(FAC_SIP, PRI_INFO, "SipXProxyCstaObserver invalid message type %d", msgType );
   }
   } // end switch (msgType)
   
   return(TRUE);
}

/* ============================ ACCESSORS ================================= */

/* ============================ INQUIRY =================================== */

/* //////////////////////////// PROTECTED ///////////////////////////////// */

/* //////////////////////////// PRIVATE /////////////////////////////////// */

/* ============================ FUNCTIONS ================================= */

