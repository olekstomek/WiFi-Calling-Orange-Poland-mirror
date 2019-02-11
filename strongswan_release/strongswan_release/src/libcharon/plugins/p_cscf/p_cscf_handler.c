/*
 * Copyright (C) 2016 Tobias Brunner
 * Hochschule fuer Technik Rapperswil
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.  See <http://www.fsf.org/copyleft/gpl.txt>.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 */

#include "p_cscf_handler.h"

#include <networking/host.h>
#include <utils/debug.h>

typedef struct private_p_cscf_handler_t private_p_cscf_handler_t;

/**
 * Private data
 */
struct private_p_cscf_handler_t {

	/**
	 * Public interface
	 */
	p_cscf_handler_t public;
};

#define OLP_MAx_IP_LENGTH 32  // actually less than that
void olp_save_pcscf_server_ip(host_t *net)
{
    char bufferx[OLP_MAx_IP_LENGTH] = {0};
    chunk_t addr = net->get_address(net);
    char *buffer = (char*)malloc(addr.len);
    strcpy(buffer, addr.ptr);

    snprintf(bufferx, OLP_MAx_IP_LENGTH, "%d.%d.%d.%d", buffer[0], buffer[1], buffer[2], buffer[3]);
    free(buffer);

    char* prevValue = lib->settings->get_str(lib->settings, "olp.pcscf.serverip", NULL);
    if (!prevValue)
    {
        lib->settings->set_str(lib->settings, "olp.pcscf.serverip", bufferx);
    }
    else // append this ip to previously saved address(es)
    {
        const char *separator = ",";
        char *buffer2 = (char*)calloc(OLP_MAx_IP_LENGTH + strlen(separator) + strlen(prevValue), sizeof(char));
        strcpy(buffer2, prevValue);
        strcat(buffer2, separator);
        strcat(buffer2, bufferx);
        lib->settings->set_str(lib->settings, "olp.pcscf.serverip", buffer2);
        free(buffer2);
    }
}

METHOD(attribute_handler_t, handle, bool,
	private_p_cscf_handler_t *this, ike_sa_t *ike_sa,
	configuration_attribute_type_t type, chunk_t data)
{
	host_t *server;
	int family = AF_INET6;

	switch (type)
	{
		case P_CSCF_IP4_ADDRESS:
			family = AF_INET;
			/* fall-through */
			server = host_create_from_chunk(family, data, 0);
			if (!server)
			{
				DBG1(DBG_CFG, "received invalid P-CSCF server IP");
				return FALSE;
			}
			DBG1(DBG_CFG, "received P-CSCF server IP %H", server);
			olp_save_pcscf_server_ip(server);
			server->destroy(server);
			return TRUE;
		case P_CSCF_IP6_ADDRESS:
            DBG1(DBG_CFG, "IPv6 support disabled (olp)");
			return FALSE;
		default:
			return FALSE;
	}
}

METHOD(attribute_handler_t, release, void,
	private_p_cscf_handler_t *this, ike_sa_t *ike_sa,
	configuration_attribute_type_t type, chunk_t data)
{
	switch (type)
	{
		case P_CSCF_IP4_ADDRESS:
		case P_CSCF_IP6_ADDRESS:
			/* nothing to do as we only log the server IPs */
			break;
		default:
			break;
	}
}

/**
 * Data for attribute enumerator
 */
typedef struct {
	enumerator_t public;
	int request_ipv4;
	bool request_ipv6;
} attr_enumerator_t;

METHOD(enumerator_t, enumerate_attrs, bool,
	attr_enumerator_t *this, configuration_attribute_type_t *type,
	chunk_t *data)
{
	if (this->request_ipv4>0)
	{
		*type = P_CSCF_IP4_ADDRESS;
		*data = chunk_empty;
		this->request_ipv4--;
		return TRUE;
	}
	if (this->request_ipv6)
	{
		*type = P_CSCF_IP6_ADDRESS;
		*data = chunk_empty;
		this->request_ipv6 = FALSE;
		return TRUE;
	}
	
	return FALSE;
}

/**
 * Check if the given host has a matching address family
 */
static bool is_family(host_t *host, int *family)
{
	return host->get_family(host) == *family;
}

/**
 * Check if a list has a host of a given family
 */
static bool has_host_family(linked_list_t *list, int family)
{
	return list->find_first(list, (void*)is_family, NULL, &family) == SUCCESS;
}

METHOD(attribute_handler_t, create_attribute_enumerator, enumerator_t *,
	private_p_cscf_handler_t *this, ike_sa_t *ike_sa,
	linked_list_t *vips)
{
	attr_enumerator_t *enumerator;

	if (ike_sa->get_version(ike_sa) == IKEV1)
	{
		return enumerator_create_empty();
	}

	INIT(enumerator,
		.public = {
			.enumerate = (void*)_enumerate_attrs,
			.destroy = (void*)free,
		},
	);
	
	 if (has_host_family(vips, AF_INET))
	    enumerator->request_ipv4 = 2;
	
	if (lib->settings->get_bool(lib->settings, "%s.plugins.p-cscf.enable.%s",
								FALSE, lib->ns, ike_sa->get_name(ike_sa)))
	{
		enumerator->request_ipv4 = has_host_family(vips, AF_INET);
		enumerator->request_ipv6 = has_host_family(vips, AF_INET6);
	}
	return &enumerator->public;
}

METHOD(p_cscf_handler_t, destroy, void,
	private_p_cscf_handler_t *this)
{
	free(this);
}

/**
 * See header
 */
p_cscf_handler_t *p_cscf_handler_create()
{
	private_p_cscf_handler_t *this;

	INIT(this,
		.public = {
			.handler = {
				.handle = _handle,
				.release = _release,
				.create_attribute_enumerator = _create_attribute_enumerator,
			},
			.destroy = _destroy,
		},
	);

	return &this->public;
}
