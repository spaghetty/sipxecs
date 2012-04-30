/*
 * Copyright (c) 2011 eZuce, Inc. All rights reserved.
 * Contributed to SIPfoundry under a Contributor Agreement
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */



#include "sqa/ServiceOptions.h"
#include "sqa/StateQueueAgent.h"
#include "sqa/StateQueueDriverTest.h"

int main(int argc, char** argv)
{
  ServiceOptions service(argc, argv, "StateQueueAgent", "1.0.0", "Copyright Ezuce Inc. (All Rights Reserved)");
  service.addDaemonOptions();
  service.addOptionString("zmq-subscription-address", ": Address where to subscribe for events.");
  service.addOptionString("sqa-control-port", ": Port where to send control commands.");
  service.addOptionString("sqa-control-address", ": Address where to send control commands.");
  service.addOptionString("publish-entity-oplog-config", ": Set this value if you want SQA to publish oplogs from mongo for the IdentityDB.");
  service.addOptionFlag("test-driver", ": Set this flag if you want to run the driver unit tests to ensure proper operations.");
  


  if (!service.parseOptions() ||
          !service.hasOption("zmq-subscription-address") ||
          !service.hasOption("sqa-control-port") ||
          !service.hasOption("sqa-control-address") )
    service.displayUsage(std::cerr);

  StateQueueAgent sqa(service);
  sqa.run();

  if (service.hasOption("test-driver"))
  {
    StateQueueDriverTest test(sqa);
    if (!test.runTests())
      return -1;
    sqa.stop();
    return 0;
  }
  OS_LOG_INFO(FAC_NET, "State Queue Agent process STARTED.");
  service.waitForTerminationRequest();
  OS_LOG_INFO(FAC_NET, "State Queue Agent process TERMINATED.");
  return 0;
}
