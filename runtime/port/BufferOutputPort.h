#ifndef _LLIBY_PORT_BUFFEROUTPUTPORT_H
#define _LLIBY_PORT_BUFFEROUTPUTPORT_H

#include "AbstractPort.h"

#include <sstream>

namespace lliby
{

class BufferOutputPort : public AbstractOutputOnlyPort
{
public:
	bool isOutputPortOpen() const override
	{
		return m_open;
	}

	void closeOutputPort() override
	{
		m_open = false;
	}

	std::ostream *outputStream() override
	{
		return &m_buffer;
	}

protected:
	bool m_open = true;
	std::ostringstream m_buffer;
};

}

#endif
