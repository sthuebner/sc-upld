/*
  The dynamic bits of upload.html
*/




/*
  Updates the progress bar.
*/
function updateProgress( data ) {
    var progress = 100;
    if( data != null ) {
	var n = data.split( "/" );
	progress = Math.round( n[0] / n[1] * 100 );
	$( "#progress" ).html( "Status: " + progress + "%" );
    }
    return ( data != null && progress < 100 );
}


/*
  Makes an AJAX call to URL, passes the returned data to CALLBACK, and
  continues every INTERVALL milliseconds, as long as CALLBACK's return
  value is true.

  CALLBACK is a function of one argument. It should return TRUE or FALSE.
*/
function repeatedlyHit( url, intervall, callback ) {
    $.ajax( url,
	    { success: function( data ) {
		// if callback returns true, do it again…
		if( callback( data ) ) {
		    setTimeout( function() {
			repeatedlyHit( url, intervall, callback )
		    }, intervall )};
	    }});
}



// inspired by http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript
function UID() {
    return (((Math.random() + 1) * 0x10000) | 0).toString( 16 ).substring( 1 );
}

function initProgressContainer( fileId ) {
    $.ajax({ url: "/upload/"+ fileId,
	     headers: { accept: "application/json" },
	     success: function( data ) {
		 // set filename
		$( "#filename" ).html( "Storing as "+ data["local-file"] );
		
		 // update link
		 var link = $( "#download-link" );
		 link.attr( "href", "/upload/"+ fileId + "/file" );
	     }});
}

// keeps the ID of the currently uploaded file
var fileId = null;

$( document ).ready( function() {

    $( "#download-link" ).attr( "disabled", "disabled" );
    
    // submit form when user selected a file
    $( "#file" ).change( function() {
	// identify the file with a semi unique ID
	fileId = UID();
	$( "#upload-form" ).attr( "action", "/upload/"+ fileId );
        $( "#upload-form" ).submit();
    });

    // start progress loop a bit after form got submitted
    $( "#upload-form" ).submit( function() {
	var intervall = 1000;
	setTimeout( function() {
	    initProgressContainer( fileId );
	    repeatedlyHit( "/upload/"+ fileId +"/progress", intervall, updateProgress );
	}, intervall);

	$( "#description-form" ).attr( "action", "/upload/"+ fileId +"/description" );
    });
});
