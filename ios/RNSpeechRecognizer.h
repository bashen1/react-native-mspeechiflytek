#import <iflyMSC/iflyMSC.h>
#if __has_include("RCTBridgeModule.h")
#import "React/RCTBridgeModule.h"
#import "React/RCTEventEmitter.h"
#else
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#endif

@interface RNSpeechRecognizer : RCTEventEmitter <RCTBridgeModule, IFlySpeechRecognizerDelegate>
{
    BOOL hasListeners;
}

@property (nonatomic, strong) IFlySpeechRecognizer * iFlySpeechRecognizer;
@property (nonatomic) NSTimeInterval startTime;
@property (nonatomic) NSTimeInterval endTime;
@property (nonatomic, strong) NSMutableString * result;
@end
  
