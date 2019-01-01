#include <stdio.h>
#include <process.h>
#include <stdlib.h>
#include <string.h>
#include <direct.h>

int main(int argc, char *argv[])
{
	// find the basedir of this executable's path
	const char *quote = "\"";
	char *path = argv[0];
	char *lastSlash = strrchr(path, '\\'); // you need escape character
	// trim path to basedir incl. backslash
	path[strlen(path)- strlen(lastSlash)+1]='\0';

	int res = _chdir(path);
	printf("Chdir to '%s' results in %d.\n", path, res);

	// calculate the new length, "/C","oj_windows.bat" + all argv except $0 + "||","pause" + 1 trailing NULL
	int new_length = 2 + (argc - 1) + 2 + 1;
	// create the new param array of char arry pointers
	char **new_argv = malloc(sizeof(char *) * (new_length));
	// add default params
	new_argv[0] = "/C";
	new_argv[1] = "oj_windows.bat";

	// copy parameters except $0, which is this executable's name
	for (int i = 1; i < argc; i++) {
		// quote it as we receive the parameters fully unquoted
		char *string = malloc(strlen(quote)*2 + strlen(argv[i]) + 1);//+1 for the zero-terminator
		strcpy(string, quote);
		strcat(string, argv[i]);
		strcat(string, quote);

		//printf("add Arg %d %s\n", i+1, string);
		new_argv[i + 1] = string;
	}
	new_argv[new_length - 3] = "||";
	new_argv[new_length - 2] = "pause";
	// trail the array with a NULL entry to signal end of params
	new_argv[new_length-1] = NULL;

	// debug printout
	for (int i = 0; i < new_length; i++) {
		printf("Arg %d %s\n", i, new_argv[i]);
	}

	//const char *test[3] = { "/C", "oj_windows.bat", NULL };
	_spawnvp(P_WAIT, "cmd.exe", new_argv );
	//_execvp("cmd.exe", new_argv);
	return 0;
}

