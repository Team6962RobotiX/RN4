package frc.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.cscore.CvSink;
import edu.wpi.first.cscore.CvSource;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.hal.simulation.DutyCycleDataJNI;
import edu.wpi.first.cameraserver.*;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.motorcontrol.Spark;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.motorcontrol.VictorSP;
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import java.io.File;
import java.io.IOException;

import org.opencv.core.*;
import org.opencv.core.Mat;

import java.util.*;

public class Robot extends TimedRobot {
    // Limelight
    private boolean m_LimelightHasValidTarget = false;
    private double m_LimelightDriveCommand = 0.0;
    private double m_LimelightSteerCommand = 0.0;
    private int limelight_pipeline_blue = 4;
    private int limelight_pipeline_red = 3;

    //Joystick
    Joystick joystick;
    //private final JoystickButton m_stick_button_blue = new JoystickButton(joystick, 2);
    //private final JoystickButton m_stick_button_red = new JoystickButton(joystick, 3);

    // Drive Power
    double leftPower = 0;
    double rightPower = 0;

    //Hang
    boolean commenceHang;
    int hangStep;
    double encoderValueHold1 = null;
    double encoderValueHold2 = null;
    int stage = 1;

    // Drive Motor Controllers
    MotorControllerGroup rightBank;
    MotorControllerGroup leftBank;
    DifferentialDrive myDrive;

    // Climb Motor Controllers
    CANSparkMax frontLeftClimb;
    CANSparkMax frontRightClimb;
    CANSparkMax backLeftClimb;
    CANSparkMax backRightClimb;
    Spark leadScrews;

    // Outtake Motor Controllers
    Spark highOuttake;
    Spark lowOuttake;
    Spark transferToOuttake;
    Spark outtakeRotator;

    // Intake Motor Controllers
    Spark intakeBrush;
    Spark intakeComp;

    // Encoders
    Encoder leadScrewsEncoder;
    RelativeEncoder frontLeftClimbEncoder;
    RelativeEncoder frontRightClimbEncoder;
    RelativeEncoder backLeftClimbEncoder;
    RelativeEncoder backRightClimbEncoder;

    double leadScrewsEncoderValue;
    double frontLeftClimbEncoderValue;
    double frontRightClimbEncoderValue;
    double backLeftClimbEncoderValue;
    double backRightClimbEncoderValue;

    double transferToOuttakePower = -0.6;

    final int WIDTH = 640;
    
    @Override
    public void robotInit() {
        // Joystick
        joystick = new Joystick(0);

        rightBank = new MotorControllerGroup(
            new CANSparkMax(2, MotorType.kBrushless),
            new CANSparkMax(9, MotorType.kBrushless)
        );

        leftBank = new MotorControllerGroup(
            new CANSparkMax(1, MotorType.kBrushless),
            new CANSparkMax(4, MotorType.kBrushless)
        );
        
        frontLeftClimb = new CANSparkMax(11, MotorType.kBrushless);
        frontRightClimb = new CANSparkMax(5, MotorType.kBrushless);
        backLeftClimb = new CANSparkMax(7, MotorType.kBrushless);
        backRightClimb = new CANSparkMax(3, MotorType.kBrushless);
        leadScrews = new Spark(0);

        highOuttake = new Spark(4);
        lowOuttake = new Spark(3);
        transferToOuttake = new Spark(2);
        outtakeRotator = new Spark(1);

        intakeBrush = new Spark(6);
        intakeComp = new Spark(5);
        myDrive = new DifferentialDrive(leftBank, rightBank);

        leadScrewsEncoder = new Encoder(0, 1);
        frontLeftClimbEncoder = frontLeftClimb.getEncoder();
        frontRightClimbEncoder = frontRightClimb.getEncoder();
        backLeftClimbEncoder = backLeftClimb.getEncoder();
        backRightClimbEncoder = backRightClimb.getEncoder();
       // NetworkTableInstance.getDefault().getTable("limelight").getEntry("pipeline").setNumber(limelight_pipeline_blue);
    }

    @Override
    public void robotPeriodic() {
        //leadScrewsEncoderValue += resetEncoderValue(leadScrewsEncoder);
        frontLeftClimbEncoderValue += resetEncoderValue(frontLeftClimbEncoder);
        frontRightClimbEncoderValue += resetEncoderValue(frontRightClimbEncoder);
        backLeftClimbEncoderValue += resetEncoderValue(backLeftClimbEncoder);
        backRightClimbEncoderValue += resetEncoderValue(backRightClimbEncoder);
    }

    public double resetEncoderValue(RelativeEncoder encoder) {
        double value = encoder.getPosition();
        encoder.setPosition(0);
        return value;
    }

    @Override
    public void autonomousInit() {
       // start = System.currentTimeMillis();
    }

    @Override
    public void autonomousPeriodic() {
        
    }

    public void doIntakeTransfer() {
        transferToOuttake.set(transferToOuttakePower);
    }

    @Override
    public void teleopInit() {
        //start = System.currentTimeMillis();
        boolean commenceHang;
        int hangStep;

        
    }

   public double calcDriveCurve(double power) {
        double harshness = 8.0;

        if (power >= 1.0) {
            power = 0.99;
        }
        if (power <= -1.0) {
            power = -0.99;
        }
        if (power == 0.0) {
            return 0.0;
        }
        if (power > 0.0) {
            return Math.min(1.0, Math.max(0.0, -1 * (Math.log(1 / (power + 0) - 1) / harshness) + 0.5));
        }
        if (power < 0.0) {
            return Math.max(-1.0, Math.min(0.0, -1 * (Math.log(1 / (power + 1) - 1) / harshness) - 0.5));
        }
        return 0.0;
    }
    

    @Override
    public void teleopPeriodic() {

        if (joystick.getRawButtonPressed(1)) {
            doIntakeTransfer();
        }

        //Toggle seeking red or blue balls
       /* if (m_stick.getRawButtonPressed(8)) {
            System.out.println("Seeking Blue");
            NetworkTableInstance.getDefault().getTable("limelight").getEntry("pipeline").setNumber(limelight_pipeline_blue);
        }
      
        if (m_stick.getRawButtonPressed(9)) {
            System.out.println("Seeking Red");
            NetworkTableInstance.getDefault().getTable("limelight").getEntry("pipeline").setNumber(limelight_pipeline_red);
        }
        */
        //Update_Limelight_Tracking();
        //boolean auto = joystick.getRawButton(1);
        //leftBank.setVoltage(12);
        //rightBank.setVoltage(12);
        //long now = System.currentTimeMillis();
        // Turning speed limit
        double limitTurnSpeed = 0.5; // EDITABLE VALUE

        //Outtake
        /*if (joystick.getRawButton(1)) {
            //myDrive.tankDrive(0, 0);
            highOuttake.set(joystick.getRawAxis(1) * 0.5);
            lowOuttake.set(joystick.getRawAxis(1) * 0.5);
        }
        //Intake
        if (joystick.getRawButton(2)) {
            intakeComp.set(0.8);
            intakeBrush.set(0.8);
        }
        //Transfer
        if (joystick.getRawButton(5)) {
            transferToOuttake.set(-0.8);
        }
        */
        // Default manual Drive Values
        double joystickLValue =
                (-joystick.getRawAxis(1) + (joystick.getRawAxis(2) * limitTurnSpeed));
        double joystickRValue =
                (-joystick.getRawAxis(1) - (joystick.getRawAxis(2) * limitTurnSpeed));

        // ADDITIONAL DRIVE CODE HERE
        /*
        if (auto)
        {
            if (m_LimelightHasValidTarget)
            {
                System.out.println("Auto Drive=" + m_LimelightDriveCommand + " Steer=" + m_LimelightSteerCommand);
                m_robotDrive.arcadeDrive(m_LimelightDriveCommand,m_LimelightSteerCommand);
            }
            else
            {
            //System.out.println("Auto but no target! Drive=" + m_LimelightDriveCommand + " Steer=" + m_LimelightSteerCommand);
            m_robotDrive.arcadeDrive(0.0, 0.5);
            }
        }
        */

        
        
        // Forgive a slight turn
       // if (joystickLValue - joystickRValue < 0.2 && joystickLValue - joystickRValue > -0.2) {
         //   joystickLValue = joystickRValue;
        //}
        //double[] test = joystickToRPS(-joystick.getRawAxis(1), -joystick.getRawAxis(2));
        //double[] test2 = getDrivePower(test[0], test[1], 50);

        // Actual Drive code
        
        myDrive.tankDrive(joystickLValue, -joystickRValue);

        // HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE
        // HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE
        // HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE
        // HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE <===> HANG CODE

        // assumes that the back set (S2) of arms are slightly tilted towards the back (front set is referred to as S1)
        // max encoder rotation of robot: full back tilt: 115.0; full forward tilt: -173.0
        // max encoder extension of S1 for lowest starting point of 0: left 454.9; right: 454.9
        // max encoder extension of S2 for lowest starting point of 0: left 443.0; right: 459.4
        if( joystick.getRawButtonPressed( 4 ) ) {
            if( stage == 1 ) { // extends S2 in front of bar 2
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = backLeftClimbEncoder.getPosition();
                    encoderValueHold2 = backRightClimbEncoder.getPosition();
                }
                if( motionset( backLeftClimb, backLeftClimbEncoder, encoderValueHold1, 443.0, [ 0, 443.0 ], 1 ) && motionset( backRightClimb, backRightClimbEncoder, encoderValueHold2, 459.4, [ 0, 459.4 ], 1 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 2 ) { // slightly rotates S2's hooks to be above bar 2
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = leadScrewsEncoder.getDistance();
                    encoderValueHold2 = leadScrewsEncoder.getDistance();
                }
                if( motionset( leadScrews, leadScrewsEncoder, encoderValueHold1, /* relative encoder target value */, [ -173.0, 115.0 ], 0.5 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 3 ) { // retracts S2 to hook onto bar 2 and pull the robot up
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = backLeftClimbEncoder.getPosition();
                    encoderValueHold2 = backRightClimbEncoder.getPosition();
                }
                if( motionset( backLeftClimb, backLeftClimbEncoder, encoderValueHold1, /* relative encoder target value */, [ 0, 443.0 ], -1 ) && motionset( backRightClimb, backRightClimbEncoder, encoderValueHold2, /* encoder target value */, [ 0, 459.4 ], -1 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 4 ) { // rotates the robot so S1 is in a position to extend above but not run into bar 3
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = leadScrewsEncoder.getDistance();
                    encoderValueHold2 = leadScrewsEncoder.getDistance();
                }
                if( motionset( leadScrews, leadScrewsEncoder, encoderValueHold1, /* relative encoder target value */, [ -173.0, 115.0 ], -0.5 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 5 ) { // extends S1 above bar 3
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = frontLeftClimbEncoder.getPosition();
                    encoderValueHold2 = frontRightClimbEncoder.getPosition();
                }
                if( motionset( frontLeftClimb, frontLeftClimbEncoder, encoderValueHold1, /* relative encoder target value */, [ 0, 454.9 ], 1 ) && motionset( frontRightClimb, frontRightClimbEncoder, encoderValueHold2, /* encoder target value */, [ 0, 454.9 ], 1 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 6 ) { // rotates the robot slightly, so that when S1 retracts, it hooks onto bar 3
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = leadScrewsEncoder.getDistance();
                    encoderValueHold2 = leadScrewsEncoder.getDistance();
                }
                if( motionset( leadScrews, leadScrewsEncoder, encoderValueHold1, /* relative encoder target value */, [ -173.0, 115.0 ], -0.5 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 7 ) { // retracts S1 slightly, to grip onto bar 3 in preparation for extending S2
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = frontLeftClimbEncoder.getPosition();
                    encoderValueHold2 = frontRightClimbEncoder.getPosition();
                }
                if( motionset( frontLeftClimb, frontLeftClimbEncoder, encoderValueHold1, /* relative encoder target value */, [ 0, 454.9 ], -1 ) && motionset( frontRightClimb, frontRightClimbEncoder, encoderValueHold2, /* encoder target value */, [ 0, 454.9 ], -1 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 8 ) { // extend S2, enough so that it unhooks from bar 2
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = backLeftClimbEncoder.getPosition();
                    encoderValueHold2 = backRightClimbEncoder.getPosition();
                }
                if( motionset( backLeftClimb, backLeftClimbEncoder, encoderValueHold1, /* relative encoder target value */, [ 0, 443.0 ], 1 ) && motionset( backRightClimb, backRightClimbEncoder, encoderValueHold2, /* encoder target value */, [ 0, 459.4 ], 1 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 9 ) { // rotate robot slightly so S2 hooks can retract
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = leadScrewsEncoder.getDistance();
                    encoderValueHold2 = leadScrewsEncoder.getDistance();
                }
                if( motionset( leadScrews, leadScrewsEncoder, encoderValueHold1, /* relative encoder target value */, [ -173.0, 115.0 ], -0.5 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 10 ) { // retracts S2 to clear bar 2, so it can rotate to be in position to grab bar 4
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = backLeftClimbEncoder.getPosition();
                    encoderValueHold2 = backRightClimbEncoder.getPosition();
                }
                if( motionset( backLeftClimb, backLeftClimbEncoder, encoderValueHold1, /* relative encoder target value */, [ 0, 443.0 ], -1 ) && motionset( backRightClimb, backRightClimbEncoder, encoderValueHold2, /* encoder target value */, [ 0, 459.4 ], -1 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 11 ) { // rotate S2 so that when it extends after S1 retracts, the hook does not run into bar 4
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = leadScrewsEncoder.getDistance();
                    encoderValueHold2 = leadScrewsEncoder.getDistance();
                }
                if( motionset( leadScrews, leadScrewsEncoder, encoderValueHold1, /* relative encoder target value */, [ -173.0, 115.0 ], 0.5 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 12 ) { // retract S1 to prepare for grabbing bar 4
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = frontLeftClimbEncoder.getPosition();
                    encoderValueHold2 = frontRightClimbEncoder.getPosition();
                }
                if( motionset( frontLeftClimb, frontLeftClimbEncoder, encoderValueHold1, /* relative encoder target value */, [ 0, 454.9 ], -1 ) && motionset( frontRightClimb, frontRightClimbEncoder, encoderValueHold2, /* encoder target value */, [ 0, 454.9 ], -1 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 13 ) { // extend S2 so that when it rotates, the hook is slightly beyond bar 4
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = backLeftClimbEncoder.getPosition();
                    encoderValueHold2 = backRightClimbEncoder.getPosition();
                }
                if( motionset( backLeftClimb, backLeftClimbEncoder, encoderValueHold1, /* relative encoder target value */, [ 0, 443.0 ], 1 ) && motionset( backRightClimb, backRightClimbEncoder, encoderValueHold2, /* encoder target value */, [ 0, 459.4 ], 1 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 14 ) { // slightly rotate S2 to prepare for hooking onto bar 4
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = leadScrewsEncoder.getDistance();
                    encoderValueHold2 = leadScrewsEncoder.getDistance();
                }
                if( motionset( leadScrews, leadScrewsEncoder, encoderValueHold1, /* relative encoder target value */, [ -173.0, 115.0 ], 0.5 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 15 ) { // slightly retract S2, to hook onto bar 4 and prepare to unhook from bar 3
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = backLeftClimbEncoder.getPosition();
                    encoderValueHold2 = backRightClimbEncoder.getPosition();
                }
                if( motionset( backLeftClimb, backLeftClimbEncoder, encoderValueHold1, /* relative encoder target value */, [ 0, 443.0 ], -1 ) && motionset( backRightClimb, backRightClimbEncoder, encoderValueHold2, /* encoder target value */, [ 0, 459.4 ], -1 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 16 ) { // extend S1, enough so that it unhooks from bar 3
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = frontLeftClimbEncoder.getPosition();
                    encoderValueHold2 = frontRightClimbEncoder.getPosition();
                }
                if( motionset( frontLeftClimb, frontLeftClimbEncoder, encoderValueHold1, /* relative encoder target value */, [ 0, 454.9 ], 1 ) && motionset( frontRightClimb, frontRightClimbEncoder, encoderValueHold2, /* encoder target value */, [ 0, 454.9 ], 1 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 17 ) { // rotate robot slightly, so that S1 can retract
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = leadScrewsEncoder.getDistance();
                    encoderValueHold2 = leadScrewsEncoder.getDistance();
                }
                if( motionset( leadScrews, leadScrewsEncoder, encoderValueHold1, /* relative encoder target value */, [ -173.0, 115.0 ], 0.5 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 18 ) { // retract S1 so it may clear bar 3 in rotating to a stable position
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = frontLeftClimbEncoder.getPosition();
                    encoderValueHold2 = frontRightClimbEncoder.getPosition();
                }
                if( motionset( frontLeftClimb, frontLeftClimbEncoder, encoderValueHold1, /* relative encoder target value */, [ 0, 454.9 ], -1 ) && motionset( frontRightClimb, frontRightClimbEncoder, encoderValueHold2, /* encoder target value */, [ 0, 454.9 ], -1 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            } else if( stage == 19 ) { // rotate the robot into a normal position
                if( !encoderValueHold1 && !encoderValueHold1 ) {
                    encoderValueHold1 = leadScrewsEncoder.getDistance();
                    encoderValueHold2 = leadScrewsEncoder.getDistance();
                }
                if( motionset( leadScrews, leadScrewsEncoder, encoderValueHold1, /* relative encoder target value */, [ -173.0, 115.0 ], -0.5 ) ) {
                    encoderValueHold1 = null;
                    encoderValueHold2 = null;
                    stage++;
                }
            }
        }
    }

    public boolean motionset( motorObject, encoderObject, encoderInitialValue, relativeEncoderTargetValue, encoderLimits, speed ) { // returns the status of the movement: false if target not yet reached & vice versa
        if( encoderLimits[ 0 ] < encoderObject.getPosition() && encoderLimits[ 1 ] > encoderObject.getPosition() ) && ( encoderInitialValue > encoderInitialValue + relativeEncoderTargetValue && encoderObject.getPosition() > encoderInitialValue + relativeEncoderTargetValue || encoderInitialValue < encoderInitialValue + relativeEncoderTargetValue && encoderObject.getPosition() < encoderInitialValue + relativeEncoderTargetValue ) ) {
            if( motorObject.get() != speed ) motorObject.set( speed );
            return false;
        } else {
            motorObject.set( 0 );
            return true;
        }
    }

    public boolean rotationset( motorObject, encoderObject, encoderInitialValue, relativeEncoderTargetValue, encoderLimits, speed ) { // returns the status of the movement: false if target not yet reached & vice versa
        if( ( encoderLimits[ 0 ] < encoderObject.getDistance() && encoderLimits[ 1 ] > encoderObject.getDistance() ) && ( encoderInitialValue > encoderInitialValue + relativeEncoderTargetValue && encoderObject.getDistance() > encoderInitialValue + relativeEncoderTargetValue || encoderInitialValue < encoderInitialValue + relativeEncoderTargetValue && encoderObject.getDistance() < encoderInitialValue + relativeEncoderTargetValue ) ) {
            if( motorObject.get() != speed ) motorObject.set( speed );
            return false;
        } else {
            motorObject.set( 0 );
            return true;
        }
    }

    @Override
    public void testPeriodic() {}
/*
    public void Update_Limelight_Tracking()
    {
        // These numbers must be tuned for your Robot!  Be careful!
        final double STEER_K = 0.06;                    // how hard to turn toward the target (initial 0.03)
        final double DRIVE_K = 0.26;                    // how hard to drive fwd toward the target (initial 0.26)
        final double DESIRED_TARGET_AREA = 13.0;        // Area of the target when the robot reaches the wall
        final double MAX_DRIVE = 0.7;                   // Simple speed limit so we don't drive too fast
        final double MIN_STEER = -0.7;
        final double MAX_STEER = 0.7;

       /* double tv = NetworkTableInstance.getDefault().getTable("limelight").getEntry("tv").getDouble(0);
        double tx = NetworkTableInstance.getDefault().getTable("limelight").getEntry("tx").getDouble(0);
        double ty = NetworkTableInstance.getDefault().getTable("limelight").getEntry("ty").getDouble(0);
        double ta = NetworkTableInstance.getDefault().getTable("limelight").getEntry("ta").getDouble(0);

    if (tv < 1.0)
        {
          m_LimelightHasValidTarget = false;
          m_LimelightDriveCommand = 0.0;
          m_LimelightSteerCommand = 0.0;
          return;
        }

        m_LimelightHasValidTarget = true;

        // Start with proportional steering
        double steer_cmd = tx * STEER_K;

        // try to drive forward until the target area reaches our desired area
        double drive_cmd = (DESIRED_TARGET_AREA - ta) * DRIVE_K;

        // don't let the robot drive too fast into the goal
        if (drive_cmd > MAX_DRIVE)
        {
          drive_cmd = MAX_DRIVE;
        }

        if (steer_cmd > MAX_STEER) {
          steer_cmd = MAX_STEER;
        }

        if (steer_cmd < MIN_STEER) {
          steer_cmd = MIN_STEER;
        }

        m_LimelightSteerCommand = steer_cmd;
        m_LimelightDriveCommand = -drive_cmd;

        //System.out.println("Steering Tx=" + tx + " Steer=" + steer_cmd);
        //System.out.println("Driving Ta=" + ta + " Drive=" + drive_cmd);

        //m_LimelightDriveCommand = 0.0;
        //m_LimelightSteerCommand = 0.0;
        
    }
    */
    
}/*
    public double[] joystickToRPS(double lateral, double rotational){
        double leftRotationSpeed = 5*lateral - (((Math.abs(rotational) < 0.2) ? 0 : (rotational/Math.abs(rotational))*(Math.abs(rotational)-0.2))/2);
        double rightRotationSpeed = 5*lateral + (((Math.abs(rotational) < 0.2) ? 0 : (rotational/Math.abs(rotational))*(Math.abs(rotational)-0.2))/2);
        if((leftRotationSpeed < 0.1 && rightRotationSpeed <0.1) && (leftRotationSpeed > -0.1 && rightRotationSpeed > -0.1)) return new double[]{0,0};
        return new double[]{leftRotationSpeed,rightRotationSpeed};
    }
    */

    /*public double[] getDrivePower(double leftRotationSpeed, double rightRotationSpeed, double div) {
        double encoder1RotationSpeed = (encoderChange[0] / 256) / (System.currentTimeMillis() - encoderChangeTime) * 1000; // rotations per sec
        double encoder2RotationSpeed = (encoderChange[1] / 256) / (System.currentTimeMillis() - encoderChangeTime) * 1000; // rotations per sec
        double leftPowerOutput = leftPower;
        double rightPowerOutput = rightPower;
        if(Math.abs(leftRotationSpeed-encoder1RotationSpeed) >= 1){
          leftPowerOutput = approx(leftRotationSpeed);
          rightPowerOutput = approx(rightRotationSpeed);
        }else{
          leftPowerOutput += calcDriveCurve(leftRotationSpeed-encoder1RotationSpeed)/div;
          rightPowerOutput += calcDriveCurve(rightRotationSpeed-encoder2RotationSpeed)/div;
        }
        return new double[]{leftPowerOutput, rightPowerOutput};
      }
      */
