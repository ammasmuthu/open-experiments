#!/usr/bin/perl

package Sling::Content;

=head1 NAME

Content - content related functionality for Sakai implemented over rest
APIs.

=head1 ABSTRACT

Perl library providing a layer of abstraction to the REST content methods

=head2 Methods

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::ContentUtil;
use Sling::Util;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a Content object.

=cut

sub new {
    my ( $class, $url, $lwpUserAgent ) = @_;
    die "url not defined!" unless defined $url;
    die "no lwp user agent provided!" unless defined $lwpUserAgent;
    my $response;
    my $content = { BaseURL => "$url",
                    LWP => $lwpUserAgent,
		    Message => "",
		    Response => \$response };
    bless( $content, $class );
    return $content;
}
#}}}

#{{{sub set_results
sub set_results {
    my ( $content, $message, $response ) = @_;
    $content->{ 'Message' } = $message;
    $content->{ 'Response' } = $response;
    return 1;
}
#}}}

#{{{sub upload_file
sub upload_file {
    my ( $content, $localPath, $remotePath, $filename, $log ) = @_;
    my $res = ${ $content->{ 'LWP' } }->request( Sling::Util::string_to_request(
        Sling::ContentUtil::upload_file_setup( $content->{ 'BaseURL' }, $localPath, $remotePath, $filename ) ) );
    my $success = Sling::ContentUtil::upload_file_eval( \$res );
    my $basename = $localPath;
    $basename =~ s/^(.*\/)([^\/]*)$/$2/;
    my $remoteDest = $remotePath . ( $filename !~ /^$/ ? "/$filename" : "/$basename" );
    my $message = "Content: \"$localPath\" upload to \"$remoteDest\" ";
    $message .= ( $success ? "succeeded!" : "failed!" );
    $content->set_results( "$message", \$res );
    Sling::Util::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub upload_from_file
sub upload_from_file {
    my ( $content, $file, $forkId, $numberForks, $log ) = @_;
    my $count = 0;
    open ( FILE, $file );
    while ( <FILE> ) {
        if ( $forkId == ( $count++ % $numberForks ) ) {
            chomp;
	    $_ =~ /^(.*?),(.*?)$/;
	    my $localPath = $1;
	    my $remotePath = $2;
	    if ( defined $localPath && defined $remotePath ) {
	        $content->upload_file( $localPath, $remotePath, "", $log );
		Sling::Util::print_lock( $content->{ 'Message' } ) if ( ! defined $log );
	    }
	    else {
	        print "ERROR: Problem parsing content to add: \"$_\"\n";
	    }
	}
    }
    close ( FILE ); 
    return 1;
}
#}}}

1;