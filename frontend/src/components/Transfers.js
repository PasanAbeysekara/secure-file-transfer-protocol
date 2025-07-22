import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { initiateTransfer, getTransferStatus, downloadFile } from '../services/api';

const statusStyles = {
    PENDING: 'bg-yellow-100 text-yellow-800',
    PROCESSING: 'bg-blue-100 text-blue-800',
    COMPLETED: 'bg-green-100 text-green-800',
    FAILED: 'bg-red-100 text-red-800',
};

const Transfers = () => {
    const { user, token, logout } = useAuth();
    const [file, setFile] = useState(null);
    const [receiver, setReceiver] = useState('');
    const [trackedTransfers, setTrackedTransfers] = useState([]);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');

    const handleFileChange = (e) => {
        setFile(e.target.files[0]);
    };

    const handleInitiateTransfer = async (e) => {
        e.preventDefault();
        if (!file || !receiver) {
            setError('Please select a file and specify a receiver.');
            return;
        }
        setError('');
        setMessage('Initiating transfer...');
        try {
            const response = await initiateTransfer(file, receiver, token);
            setMessage(response.message);
            const newTransfer = await getTransferStatus(response.transferId, token);
            setTrackedTransfers(prev => [newTransfer, ...prev.filter(t => t.id !== newTransfer.id)]);
            setFile(null);
            e.target.reset(); // Reset form fields
        } catch (err) {
            setError(err.message);
            setMessage('');
        }
    };

    const handleDownload = async (transferId) => {
        try {
            const { blob, filename } = await downloadFile(transferId, token);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
        } catch (err) {
            setError(`Failed to download file: ${err.message}`);
        }
    };

    useEffect(() => {
        const pendingTransfers = trackedTransfers.filter(
            t => t.status === 'PENDING' || t.status === 'PROCESSING'
        );
        if (pendingTransfers.length === 0) return;

        const intervalId = setInterval(async () => {
            const updates = await Promise.all(
                trackedTransfers.map(async t => {
                    if (t.status === 'PENDING' || t.status === 'PROCESSING') {
                        try {
                            return await getTransferStatus(t.id, token);
                        } catch (e) {
                            return { ...t, status: 'FAILED', failureReason: 'Polling failed' };
                        }
                    }
                    return t;
                })
            );
            setTrackedTransfers(updates);
        }, 5000); // Poll every 5 seconds

        return () => clearInterval(intervalId);
    }, [trackedTransfers, token]);

    return (
        <div className="min-h-screen bg-gray-50">
            <header className="bg-white shadow-sm">
                <div className="container mx-auto px-4 py-4 flex justify-between items-center">
                    <h1 className="text-2xl font-bold text-gray-800">Secure Transfer</h1>
                    <div className="flex items-center space-x-4">
                        <span className="text-gray-600">
                            Welcome, <strong className="font-medium text-indigo-600">{user?.username}</strong>!
                        </span>
                        <button onClick={logout} className="px-3 py-1 text-sm font-medium text-white bg-red-600 rounded-md hover:bg-red-700 transition-colors">
                            Logout
                        </button>
                    </div>
                </div>
            </header>

            <main className="container mx-auto p-4 md:p-6">
                <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
                    {/* New Transfer Card */}
                    <div className="lg:col-span-2 bg-white p-6 rounded-lg shadow-md">
                        <h2 className="text-xl font-semibold text-gray-700 border-b pb-3 mb-4">
                            Initiate New Transfer
                        </h2>
                        <form onSubmit={handleInitiateTransfer} className="space-y-4">
                            <div>
                                <label htmlFor="file-upload" className="block text-sm font-medium text-gray-700">Select File</label>
                                <input id="file-upload" type="file" onChange={handleFileChange} required className="mt-1 block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"/>
                            </div>
                            <div>
                                <label htmlFor="receiver" className="block text-sm font-medium text-gray-700">Recipient Username</label>
                                <input id="receiver" type="text" value={receiver} onChange={(e) => setReceiver(e.target.value)} placeholder="e.g., bob" required className="w-full px-3 py-2 mt-1 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"/>
                            </div>
                            <button type="submit" className="w-full px-4 py-2 font-bold text-white bg-indigo-600 rounded-md hover:bg-indigo-700 transition-colors">
                                Send Securely
                            </button>
                            {message && <p className="text-sm text-green-600">{message}</p>}
                            {error && <p className="text-sm text-red-600">{error}</p>}
                        </form>
                    </div>

                    {/* Transfers List Card */}
                    <div className="lg:col-span-3 bg-white p-6 rounded-lg shadow-md">
                        <h2 className="text-xl font-semibold text-gray-700 border-b pb-3 mb-4">
                            Tracked Transfers
                        </h2>
                        <div className="space-y-4 max-h-[60vh] overflow-y-auto">
                            {trackedTransfers.length === 0 ? (
                                <p className="text-gray-500">No transfers initiated in this session.</p>
                            ) : (
                                trackedTransfers.map(t => (
                                    <div key={t.id} className="p-4 border border-gray-200 rounded-lg">
                                        <div className="flex justify-between items-start">
                                            <div className="flex-1">
                                                <p className="font-semibold text-gray-800">{t.fileName}</p>
                                                <p className="text-sm text-gray-500">
                                                    {t.sender === user.username ? `To: ${t.receiver}` : `From: ${t.sender}`}
                                                </p>
                                            </div>
                                            <div className={`px-2 py-1 text-xs font-bold rounded-full ${statusStyles[t.status]}`}>
                                                {t.status}
                                            </div>
                                        </div>
                                        {t.status === 'FAILED' && <p className="mt-2 text-xs text-red-600">Reason: {t.failureReason}</p>}
                                        {t.status === 'COMPLETED' && t.receiver === user.username && (
                                            <div className="mt-3 text-right">
                                                <button onClick={() => handleDownload(t.id)} className="px-3 py-1 text-sm font-medium text-white bg-green-600 rounded-md hover:bg-green-700 transition-colors">
                                                    Download
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                </div>
            </main>
        </div>
    );
};

export default Transfers;