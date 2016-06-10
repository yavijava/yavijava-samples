/*================================================================================
Copyright (c) 2008 VMware, Inc. All Rights Reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, 
this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice, 
this list of conditions and the following disclaimer in the documentation 
and/or other materials provided with the distribution.

 * Neither the name of VMware, Inc. nor the names of its contributors may be used
to endorse or promote products derived from this software without specific prior 
written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL VMWARE, INC. OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.
================================================================================*/

package com.vmware.vim25.mo.samples.alarm;

import java.rmi.RemoteException;
import java.util.Calendar;

import com.vmware.vim25.Action;
import com.vmware.vim25.AlarmAction;
import com.vmware.vim25.AlarmSetting;
import com.vmware.vim25.AlarmSpec;
import com.vmware.vim25.AlarmStatusChangedEvent;
import com.vmware.vim25.AlarmTriggeringAction;
import com.vmware.vim25.DuplicateName;
import com.vmware.vim25.Event;
import com.vmware.vim25.EventFilterSpec;
import com.vmware.vim25.EventFilterSpecByEntity;
import com.vmware.vim25.EventFilterSpecByTime;
import com.vmware.vim25.EventFilterSpecRecursionOption;
import com.vmware.vim25.GroupAlarmAction;
import com.vmware.vim25.InvalidState;
import com.vmware.vim25.MethodAction;
import com.vmware.vim25.MethodActionArgument;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.SendEmailAction;
import com.vmware.vim25.StateAlarmExpression;
import com.vmware.vim25.StateAlarmOperator;
import com.vmware.vim25.mo.AlarmManager;
import com.vmware.vim25.mo.EventHistoryCollector;
import com.vmware.vim25.mo.EventManager;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.samples.SampleUtil;

/**
 * http://vijava.sf.net
 * 
 * @author Steve Jin
 */

public class CreateVmAlarm {
   public static void main(String[] args) throws Exception {
      ServiceInstance si = SampleUtil.createServiceInstance();

      String vmname = "test-vm";
      InventoryNavigator inv = new InventoryNavigator(si.getRootFolder());
      VirtualMachine vm = (VirtualMachine) inv.searchManagedEntity("VirtualMachine", vmname);
      if (vm == null) {
         System.out.println("Cannot find the VM " + vmname + "\nExiting...");
         si.getServerConnection().logout();
         return;
      }

      AlarmManager alarmMgr = si.getAlarmManager();

      AlarmSpec spec = new AlarmSpec();

      StateAlarmExpression expression = createStateAlarmExpression();
      AlarmAction emailAction = createAlarmTriggerAction(createEmailAction());
      AlarmAction methodAction = createAlarmTriggerAction(createPowerOnAction());
      GroupAlarmAction gaa = new GroupAlarmAction();
      gaa.setAction(new AlarmAction[] { emailAction, methodAction });

      String desc = "Create a VM Alarm to send email and power VM on when the VM is powered off.";
      System.out.println(desc);
      spec.setAction(gaa);
      spec.setExpression(expression);
      spec.setName("VmPowerStateAlarm");
      spec.setDescription(desc);
      spec.setEnabled(true);

      AlarmSetting as = new AlarmSetting();
      as.setReportingFrequency(0); // as often as possible
      as.setToleranceRange(0);

      spec.setSetting(as);

      try {
         alarmMgr.createAlarm(vm, spec);
      } catch (DuplicateName e) {
         // The alarm name already exists.
      }

      // create listener for this alarm
      createAlarmEventListener(si, vm);

      si.getServerConnection().logout();
   }

   static void createAlarmEventListener(ServiceInstance si, VirtualMachine vm) throws InvalidState, RuntimeFault,
         RemoteException {
      EventManager evtMgr = si.getEventManager();
      EventFilterSpec eventFilter = new EventFilterSpec();

      /*
       * only receive the event of the specified VM EventFilterSpecByEntity
       * eFilter = new EventFilterSpecByEntity();
       * eFilter.setEntity(vm.getMOR());
       * eFilter.setRecursion(EventFilterSpecRecursionOption.self);
       * eventFilter.setEntity(eFilter);
       */

      // receive the events from now on, will not get old events.
      EventFilterSpecByTime tFilter = new EventFilterSpecByTime();
      Calendar startTime = si.currentTime();
      tFilter.setBeginTime(startTime);
      eventFilter.setTime(tFilter);

      EventHistoryCollector ehc = evtMgr.createCollectorForEvents(eventFilter);

      System.out.println("Waiting for the alarm event...\n");
      vm.powerOffVM_Task(); // power off the VM

      Event[] events = null;
      boolean isReceived = false;
      while (!isReceived) {
         try {
            Thread.sleep(3000);
         } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }

         events = ehc.readNextEvents(100);
         if (events == null)
            continue;
         for (int i = 0; i < events.length; i++) {
            Event anEvent = events[i];
            System.out.println("Event Type: " + anEvent.getClass().getName());
            System.out.println("VM: " + anEvent.getVm().getName());
            System.out.println("Message: " + anEvent.getFullFormattedMessage());
            System.out.println();
            if (anEvent instanceof AlarmStatusChangedEvent) {
               if (anEvent.getFullFormattedMessage().contains("VmPowerStateAlarm")) {
                  // The outout is:
                  // Event Type: com.vmware.vim25.AlarmStatusChangedEvent
                  // VM: test-vm
                  // Message: Alarm 'VmPowerStateAlarm' on test-vm changed from
                  // Red to Green

                  // received the specified alarm event, so exit
                  isReceived = true;
                  break;
               }
            }
         }
      }
   }

   static StateAlarmExpression createStateAlarmExpression() {
      StateAlarmExpression expression = new StateAlarmExpression();
      expression.setType("VirtualMachine");
      expression.setStatePath("runtime.powerState");
      expression.setOperator(StateAlarmOperator.isEqual);
      expression.setRed("poweredOff");
      return expression;
   }

   static MethodAction createPowerOnAction() {
      MethodAction action = new MethodAction();
      action.setName("PowerOnVM_Task");
      MethodActionArgument argument = new MethodActionArgument();
      argument.setValue(null);
      action.setArgument(new MethodActionArgument[] { argument });
      return action;
   }

   static SendEmailAction createEmailAction() {
      SendEmailAction action = new SendEmailAction();
      action.setToList("huh@vmware.com");
      action.setCcList("huh@vmware.com");
      action.setSubject("Alarm - {alarmName} on {targetName}\n");
      action.setBody("Description:{eventDescription}\n" + "TriggeringSummary:{triggeringSummary}\n"
            + "newStatus:{newStatus}\n" + "oldStatus:{oldStatus}\n" + "target:{target}");
      return action;
   }

   static AlarmTriggeringAction createAlarmTriggerAction(Action action) {
      AlarmTriggeringAction alarmAction = new AlarmTriggeringAction();
      alarmAction.setYellow2red(true);
      alarmAction.setAction(action);
      return alarmAction;
   }
}