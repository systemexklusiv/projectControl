package de.systemexklusiv.sysexprojectcontrol;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.ControllerExtension;
import de.systemexklusiv.sysexprojectcontrol.controller.Control;
import de.systemexklusiv.sysexprojectcontrol.controller.Controller;

import java.util.List;
import java.util.Optional;

public class SysexProjectControlExtension extends ControllerExtension
{

   public static final int PARAMTERS_SIZE = 32;
   public static ControllerHost host;
   Controller controller;
   MidiOut midiOut;
   private MidiIn midiIn;
   CursorRemoteControlsPage remoteControls;
   protected SysexProjectControlExtension(final SysexProjectControlExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      host = getHost();

      mTransport = host.createTransport();

      midiIn = host.getMidiInPort(0);

      host.getMidiInPort(0).setMidiCallback((ShortMidiMessageReceivedCallback)msg -> onMidi0(msg));
      host.getMidiInPort(0).setSysexCallback((String data) -> onSysex0(data));
      midiOut = host.getMidiOutPort(0);

      Project project = host.getProject();
      Track rootTrackGroup = project.getRootTrackGroup();
      //var remoteControls = rootTrackGroup.createCursorRemoteControlsPage("sysex-global", 32);
      remoteControls = rootTrackGroup.createCursorRemoteControlsPage(PARAMTERS_SIZE);


//      midiOut.sendMidi(15,122,16);
//      var LOCAL_OFF = function()
//      {
//         sendChannelController(15, 122, 64);
//      }




      controller = new Controller();

      for (int index = 0; index < PARAMTERS_SIZE; index++) {
         controller.append(
                 Control.builder()
                         .index(index)
                         .status(176)
                         .data1(index)
                         .data2(0)
                         .build()
         );
         remoteControls.getParameter(index).value().addValueObserver(128, controller.at(index));
      }

      p("SysexProjectControl Initialized");
   }

   @Override
   public void flush()
   {

      controller.changed().forEach(
               c -> {
                  p("flush: " + c );
                  for (int i = 0; i < 16; i++) {
                     midiOut.sendMidi(176 + i, c.getData1() + PARAMTERS_SIZE, c.getData2());
                  }
                  c.setChanged(false);
               }
              );

   }

   /** Called when we receive short MIDI message on port 0. */
   private void onMidi0(ShortMidiMessage msg)
   {
     p(msg.toString());

     Optional<Control> control = controller.all().stream()
             .filter(c -> c.getData1() == msg.getData1()).findFirst();

     RemoteControl currentRemote = remoteControls.getParameter(control.get().getIndex());

     if (control.isPresent() && currentRemote != null) {
        p("Set remote param: " +  currentRemote.toString());
        currentRemote.value().set(msg.getData2(), 128);
     } else p("Control not present!");
   }


   public static void p(String text) {
      host.println(text);
   }
   public static void p(int text) {
      host.println(String.valueOf(text));
   }

   @Override
   public void exit()
   {
      // TODO: Perform any cleanup once the driver exits
      // For now just show a popup notification for verification that it is no longer running.
      p("SysexProjectControl Exited");
   }



   /** Called when we receive sysex MIDI message on port 0. */
   private void onSysex0(final String data) 
   {
      // MMC Transport Controls:
      if (data.equals("f07f7f0605f7"))
            mTransport.rewind();
      else if (data.equals("f07f7f0604f7"))
            mTransport.fastForward();
      else if (data.equals("f07f7f0601f7"))
            mTransport.stop();
      else if (data.equals("f07f7f0602f7"))
            mTransport.play();
      else if (data.equals("f07f7f0606f7"))
            mTransport.record();
   }

   private Transport mTransport;
}
