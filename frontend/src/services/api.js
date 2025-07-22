const API_BASE_URL = 'http://localhost:8080/api';

// Helper function for handling API responses
const handleResponse = async (response) => {
    if (!response.ok) {
        const error = await response.json().catch(() => ({ message: response.statusText }));
        throw new Error(error.message || 'An error occurred');
    }
    return response;
};

// --- Authentication ---
export const login = async (username, password) => {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
    });
    const data = await handleResponse(response).then(res => res.json());
    return data.token;
};

// --- File Transfers ---
export const initiateTransfer = async (file, receiver, token) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('receiver', receiver);

    const response = await fetch(`${API_BASE_URL}/transfers`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData,
    });
    return handleResponse(response).then(res => res.json());
};

export const getTransferStatus = async (transferId, token) => {
    const response = await fetch(`${API_BASE_URL}/transfers/${transferId}`, {
        headers: { 'Authorization': `Bearer ${token}` },
    });
    return handleResponse(response).then(res => res.json());
};

export const downloadFile = async (transferId, token) => {
    const response = await fetch(`${API_BASE_URL}/transfers/${transferId}/content`, {
        headers: { 'Authorization': `Bearer ${token}` },
    });
    await handleResponse(response);
    
    // Extract filename from Content-Disposition header
    const disposition = response.headers.get('content-disposition');
    let filename = 'downloaded-file';
    if (disposition && disposition.indexOf('attachment') !== -1) {
        const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
        const matches = filenameRegex.exec(disposition);
        if (matches != null && matches[1]) {
            filename = matches[1].replace(/['"]/g, '');
        }
    }

    const blob = await response.blob();
    return { blob, filename };
};