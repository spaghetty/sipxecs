// 
// Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.  
// Contributors retain copyright to elements licensed under a Contributor Agreement.
// Licensed to the User under the LGPL license.
// 
//////////////////////////////////////////////////////////////////////////////

#ifndef _SipXProxyCstaObserver_h_
#define _SipXProxyCstaObserver_h_

// SYSTEM INCLUDES
#include "utl/UtlString.h"
#include <os/OsQueuedEvent.h>
#include <os/OsMutex.h>
#include <os/OsTimer.h>

// APPLICATION INCLUDES
#include <os/OsServerTask.h>
#include <net/XmlRpcRequest.h>
#include <net/XmlRpcResponse.h>
#include <net/XmlRpcDispatch.h>
#include <net/Url.h>

// DEFINES
// MACROS
// EXTERNAL FUNCTIONS
// EXTERNAL VARIABLES
// CONSTANTS
// STRUCTS
// TYPEDEFS
// FORWARD DECLARATIONS
class SipUserAgent;

class MySockServ: public OsTask{
 public:
  MySockServ(int port);  
  void write(const char *buff, int len);
  virtual int run(void*);
  virtual ~MySockServ();

  //protected:
  //OsMutex mSockGuard;

 private:
  OsSocket *m_SockConn;
  OsServerSocket *m_Server;

};

/// Observe and record Call State Events in the Forking Proxy
class SipXProxyCstaObserver : public OsServerTask
{
/* //////////////////////////// PUBLIC //////////////////////////////////// */
public:
  /* remote xmlrpc url */
/* ============================ CREATORS ================================== */

   SipXProxyCstaObserver(SipUserAgent&            sipUserAgent,
			 int    port
			 );
     //:Default constructor

   virtual
   ~SipXProxyCstaObserver();
     //:Destructor

/* ============================ MANIPULATORS ============================== */
   void addConnection(OsSocket *tmp);
   virtual UtlBoolean handleMessage(OsMsg& rMsg);

/* //////////////////////////// PROTECTED ///////////////////////////////// */
protected:

/* //////////////////////////// PRIVATE /////////////////////////////////// */
private:
   SipUserAgent*              mpSipUserAgent;
   MySockServ*                mpServ;

   //OsTimer                   mFlushTimer;
   
   /// no copy constructor or assignment operator
   SipXProxyCstaObserver(const SipXProxyCstaObserver& rSipXProxyCstaObserver);
   SipXProxyCstaObserver operator=(const SipXProxyCstaObserver& rSipXProxyCstaObserver);
};

/* ============================ INLINE METHODS ============================ */

#endif  // _SipXProxyCstaObserver_h_
