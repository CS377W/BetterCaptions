//
//  ViewController.m
//  MusicTest
//
//  Created by Brie Bunge on 4/30/14.
//  Copyright (c) 2014 cs377w. All rights reserved.
//

#import "ViewController.h"
#import <MediaPlayer/MediaPlayer.h>
#import <Firebase/Firebase.h>

@interface ViewController ()

@end

@implementation ViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    NSTimer* timer = [NSTimer timerWithTimeInterval:0.3f target:self selector:@selector(sendSongInfo) userInfo:nil repeats:YES];
    [[NSRunLoop mainRunLoop] addTimer:timer forMode:NSRunLoopCommonModes];
}

- (void)sendSongInfo
{
    MPMusicPlayerController *musicPlayer = [MPMusicPlayerController iPodMusicPlayer];
    
    NSLog(@"sending... %f", musicPlayer.currentPlaybackTime);
    
    MPMediaItem * song = [musicPlayer nowPlayingItem];
    NSString * title   = [song valueForProperty:MPMediaItemPropertyTitle];
    NSString * album   = [song valueForProperty:MPMediaItemPropertyAlbumTitle];
    NSString * artist  = [song valueForProperty:MPMediaItemPropertyArtist];
 
    Firebase* f = [[Firebase alloc] initWithUrl:@"https://fiery-fire-3139.firebaseio.com/current_song"];
    [f setValue:@{@"title": title, @"album": album, @"artist": artist, @"current_time": @(musicPlayer.currentPlaybackTime)}];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
