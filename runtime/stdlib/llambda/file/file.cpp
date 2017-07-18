#include <sys/stat.h>
#include <unistd.h>

#include "binding/StringCell.h"
#include "binding/PortCell.h"

#include "core/error.h"

#include "port/FileInputPort.h"
#include "port/FileOutputPort.h"

using namespace lliby;

extern "C"
{

bool llfile_file_exists(StringCell *filePath)
{
	struct stat statBuf;

	return stat(filePath->toUtf8StdString().c_str(), &statBuf) == 0;
}

PortCell* llfile_open_input_file(World &world, StringCell *filePath)
{
	auto inputPort = new FileInputPort(filePath->toUtf8StdString());

	if (!inputPort->inputStream()->good())
	{
		delete inputPort;
		signalError(world, ErrorCategory::File, "Unable to open path for reading", {filePath});
	}

	return PortCell::createInstance(world, inputPort);
}

PortCell* llfile_open_output_file(World &world, StringCell *filePath)
{
	auto outputPort = new FileOutputPort(filePath->toUtf8StdString());

	if (!outputPort->outputStream()->good())
	{
		delete outputPort;
		signalError(world, ErrorCategory::File, "Unable to open path for write", {filePath});
	}

	return PortCell::createInstance(world, outputPort);
}

void llfile_delete_file(World &world, StringCell *filePath)
{
	if (unlink(filePath->toUtf8StdString().c_str()) != 0)
	{
		signalError(world, ErrorCategory::File, "Unable to delete path", {filePath});
	}
}

}
