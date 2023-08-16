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
   private int channel = 0;

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

      remoteControls = rootTrackGroup.createCursorRemoteControlsPage(PARAMTERS_SIZE);

      final HardwareSurface hardwareSurface = host.createHardwareSurface();

      for (int i = 0; i < PARAMTERS_SIZE; i++) {
//         remoteControls.getParameter(index).value().addValueObserver(128, controller.at(index));
         AbsoluteHardwareKnob absKnob = hardwareSurface.createAbsoluteHardwareKnob ("knob_" + i);
         absKnob.setAdjustValueMatcher (midiIn.createAbsoluteCCValueMatcher(this.channel, i));

         absKnob.addBinding(remoteControls.getParameter(i).value());

//         remoteControls.getParameter(i).addBinding(absKnob);
      }

      p("SysexProjectControl Initialized");
   }

   @Override
   public void flush()
   {


   }

   /** Called when we receive short MIDI message on port 0. */
   private void onMidi0(ShortMidiMessage msg)
   {
      p(msg.toString());

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
