#!/usr/bin/perl

package Sling::Auth;

=head1 NAME

Auth - useful utility functions for general Auth functionality.

=head1 ABSTRACT

Utility library providing useful utility functions for general Auth functionality.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::AuthUtil;
use Sling::Print;
use Sling::Request;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a User Agent.

=cut

sub new {
    my ( $class, $url, $lwpUserAgent ) = @_;
    die "url not defined!" unless defined $url;
    die "no lwp user agent provided!" unless defined $lwpUserAgent;
    my $response;
    my $auth = { BaseURL => "$url",
                 LWP => $lwpUserAgent,
		 Message => "",
		 Response => \$response };
    bless( $auth, $class );
    return $auth;
}
#}}}

#{{{sub set_results
sub set_results {
    my ( $user, $message, $response ) = @_;
    $user->{ 'Message' } = $message;
    $user->{ 'Response' } = $response;
    return 1;
}
#}}}

#{{{sub basic_login
sub basic_login {
    my ( $auth, $log ) = @_;
    my $res = ${ $auth->{ 'LWP' } }->request( Sling::Request::string_to_request(
        Sling::AuthUtil::basic_login_setup( $auth->{ 'BaseURL' } ), $auth->{ 'LWP' } ) );
    my $success = Sling::AuthUtil::basic_login_eval( \$res );
    my $message = "Basic auth log in ";
    $message .= ( $success ? "succeeded!" : "failed!" );
    $auth->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub form_login
sub form_login {
    my ( $auth, $username, $password, $log ) = @_;
    my $res = ${ $auth->{ 'LWP' } }->request( Sling::Request::string_to_request(
        Sling::AuthUtil::form_login_setup( $auth->{ 'BaseURL' }, $username, $password ), $auth->{ 'LWP' } ) );
    my $success = Sling::AuthUtil::form_login_eval( \$res );
    my $message = "Form log in as user \"$username\" ";
    $message .= ( $success ? "succeeded!" : "failed!" );
    $auth->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

1;
