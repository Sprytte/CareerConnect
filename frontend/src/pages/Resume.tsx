import { useEffect, useState } from "react";
import "../styles/Resume.css";
import { backendUrl } from "../constants";
import { useSession } from "../auth/session";
import Delete from "../assets/trash-icon.svg";
import Edit from "../assets/edit-icon.svg";
import Ok from "../assets/ok-icon.svg";
import Cancel from "../assets/cancel-icon.svg";
// import ResumeItem from "../components/ResumeItem";

type ResumeResponse = {
  id: number;
  userId: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
};

const Resume = () => {
  const session = useSession();

  const [resume, setResume] = useState<File | null>(null);
  const [resumes, setResumes] = useState<ResumeResponse[]>([]);
  const [message, setMessage] = useState("");
  const [isUploading, setIsUploading] = useState(false);
  const [isLoadingResumes, setIsLoadingResumes] = useState(true);
  const [isEditing, setIsEditing] = useState(-1); // ID of resume being edited. -1 when none
  const [newFilename, setNewFilename] = useState("");

  const loadResumes = async (userId: string) => {
    try {
      setIsLoadingResumes(true);

      const response = await fetch(
        `${backendUrl}/api/v1/resumes`,
        {
          credentials: "include",
        }
      );

      if (!response.ok) {
        const errorText = await response.text();
        setMessage(errorText || "Could not load resumes.");
        return;
      }

      const data: ResumeResponse[] = await response.json();
      setResumes(data);
    } catch (error) {
      console.error("Resume loading error:", error);
      setMessage("Could not connect to the backend.");
    } finally {
      setIsLoadingResumes(false);
    }
  };

  const enableEditing = (resumeId: number, filename: string) => {
    setIsEditing(resumeId);
    setNewFilename(getCleanedFilename(filename)[0]);
  }

  const updateResume = async (oldFilename: string) => {
    if (!newFilename || !newFilename.trim()) {
      setMessage('Please enter a valid filename.');
      return;
    }

    if (session.status !== 'signed-in') {
      setMessage("Please sign-in to make changes to your resume.");
      setIsEditing(-1);
    }

    try {
      const newName = newFilename + '.' + getCleanedFilename(oldFilename)[1];

      const response = await fetch(
        `${backendUrl}/api/v1/resumes/${isEditing}`,
        {
          method: "PUT",
          credentials: "include",
          body: newName
        }
      );

      if (response.ok && session.status === 'signed-in') {
        setMessage('Successfully changed resume name from ' + oldFilename + ' to ' + newName);
        setIsEditing(-1);
        await loadResumes(session.user.userId);
      } else if (response.status === 404) {
        setMessage("Could not find the resume.");
        setIsEditing(-1);
      } else {
        setMessage("An unexpected error has occurred. Please retry.");
      }
    } catch (error) {
      console.error("Resume loading error:", error);
      setMessage("Could not connect to the backend.");
    } finally {
      setIsLoadingResumes(false);
    }
  }

  const handleFilenameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const filename = event.target.value;
    setNewFilename(filename);
  };

  const getCleanedFilename = (filename: string) => {
    const parts = filename.split('.');
    const name = parts.slice(0, parts.length - 1).join('.');
    const extension = parts.pop() ?? '';

    return [name, extension];
  }

  const deleteResume = async (resumeId: number, filename: string) => {
    const confirmDelete = window.confirm("Are you sure you want to delete resume '" + filename + "'?");
    if(!confirmDelete)
      return;

    if (session.status !== 'signed-in') {
      setMessage("Please sign-in to make changes to your resume.");
      setIsEditing(-1);
    }

    try {
      const response = await fetch(
        `${backendUrl}/api/v1/resumes/${resumeId}`,
        {
          method: "DELETE",
          credentials: "include",
        }
      );

      if (response.ok && session.status === 'signed-in') {
        setMessage('Successfully deleted ' + filename);
        await loadResumes(session.user.userId);
      } else if (response.status === 404) {
        setMessage("Could not find the resume.");
        setIsEditing(-1);
      } else {
        setMessage("An unexpected error has occurred. Please retry.");
      }
    } catch (error) {
      console.error("Resume loading error:", error);
      setMessage("Could not connect to the backend.");
    } finally {
      setIsLoadingResumes(false);
    }
  }

  useEffect(() => {
    if (session.status === "signed-in") {
      loadResumes(session.user.userId);
    } else if (session.status === "signed-out") {
      setResumes([]);
      setIsLoadingResumes(false);
    }
  }, [session]);

  const validateAndSetResume = (selectedFile: File) => {
    const validExtensions = [".pdf", ".docx"];
    const fileName = selectedFile.name.toLowerCase();

    const isValidFile = validExtensions.some((extension) =>
      fileName.endsWith(extension)
    );

    if (!isValidFile) {
      setResume(null);
      setMessage("Please select a PDF or DOCX file.");
      return;
    }

    setResume(selectedFile);
    setMessage("Resume selected and ready to upload.");
  };

  const handleFileChange = (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const selectedFile = event.target.files?.[0];

    if (selectedFile) {
      validateAndSetResume(selectedFile);
    }
  };

  const handleDragOver = (
    event: React.DragEvent<HTMLDivElement>
  ) => {
    event.preventDefault();
  };

  const handleDrop = (
    event: React.DragEvent<HTMLDivElement>
  ) => {
    event.preventDefault();

    const droppedFile = event.dataTransfer.files?.[0];

    if (droppedFile) {
      validateAndSetResume(droppedFile);
    }
  };


  const handleUpload = async (
    event: React.FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault();

    if (session.status !== "signed-in") {
      setMessage("Please sign in before uploading a resume.");
      return;
    }

    if (!resume) {
      setMessage("Please select a resume first.");
      return;
    }

    const userId = session.user.userId;

    const formData = new FormData();
    formData.append("file", resume);

    try {
      setIsUploading(true);
      setMessage("Uploading resume...");

      const response = await fetch(
        `${backendUrl}/api/v1/resumes`,
        {
          method: "POST",
          body: formData,
          credentials: "include",
        }
      );

      if (!response.ok) {
        const errorText = await response.text();
        setMessage(errorText || "Resume upload failed.");
        return;
      }

      const data: ResumeResponse = await response.json();

      setMessage(`Successfully uploaded ${data.fileName}.`);
      setResume(null);

      await loadResumes(userId);
    } catch (error) {
      console.error("Resume upload error:", error);
      setMessage("Could not connect to the backend.");
    } finally {
      setIsUploading(false);
    }
  };

  if (session.status === "loading") {
    return (
      <section className="resume">
        <div className="resume-container">
          <p className="resume-message">Loading account...</p>
        </div>
      </section>
    );
  }

  if (session.status === "signed-out") {
    return (
      <section className="resume">
        <div className="resume-container">
          <h2 className="resume-title">Resume Management</h2>
          <p className="resume-message">
            Please sign in to manage your resumes.
          </p>
        </div>
      </section>
    );
  }

  if (session.status === "unavailable") {
    return (
      <section className="resume">
        <div className="resume-container">
          <h2 className="resume-title">Resume Management</h2>
          <p className="resume-message">
            Account information is currently unavailable.
          </p>
        </div>
      </section>
    );
  }

  return (
    <section className="resume">
      <div className="resume-container">
        <h2 className="resume-title">Resume Management</h2>

        <p className="resume-subtitle">
          Upload and manage your resumes for CareerConnect.
        </p>

        <form onSubmit={handleUpload}>
          <div
            className="resume-upload-box"
            onDragOver={handleDragOver}
            onDrop={handleDrop}
          >
            <label className="resume-upload-label">
              Drag and drop your resume here or select a file
            </label>

            <input
              type="file"
              accept=".pdf,.docx"
              onChange={handleFileChange}
            />
          </div>

          {resume && (
            <div className="resume-selected-file">
              Selected file: <strong>{resume.name}</strong>
            </div>
          )}

          <button
            className="resume-upload-button"
            type="submit"
            disabled={isUploading}
          >
            {isUploading ? "Uploading..." : "Upload Resume"}
          </button>

          {message && (
            <p className="resume-message">{message}</p>
          )}
        </form>

        <div className="resume-list">
          <h3>Your Resumes</h3>

          {isLoadingResumes ? (
            <p className="resume-message">Loading resumes...</p>
          ) : resumes.length === 0 ? (
            <p className="resume-message">
              You haven't uploaded any resumes yet.
            </p>
          ) : (
            resumes.map((uploadedResume) => (
              <div className="resume-selected-file" key={uploadedResume.id}>
                <div>
                  {isEditing === uploadedResume.id?
                    <div style={{display: "flex"}}>
                      <input  id="editResumeInput" 
                              value={newFilename}
                              onChange={handleFilenameChange}>
                      </input>
                      <strong>.{getCleanedFilename(uploadedResume.fileName)[1]}</strong>

                      <div className="resume-icons" style={{marginLeft: "10px"}}>
                        <button className="icon-green" onClick={() => updateResume(uploadedResume.fileName)}>
                          <img src={ Ok } width={14} height={14}/>
                        </button>
                        <button className="icon-red" onClick={() => setIsEditing(-1)}>
                          <img src={ Cancel } width={14}/>
                        </button>
                      </div>
                    </div>
                    : <strong>{uploadedResume.fileName}</strong>
                  }
                  
                  <div>
                    {(uploadedResume.sizeBytes / 1024).toFixed(1)} KB
                  </div>

                  <div>
                    Uploaded:{" "}
                    {new Date(
                      uploadedResume.uploadedAt
                    ).toLocaleDateString()}
                  </div>
                </div>
                <div className="resume-icons">
                  {isEditing !== uploadedResume.id && 
                  <button className="icon-blue" onClick={() => enableEditing(uploadedResume.id, uploadedResume.fileName)}>
                    <img src={ Edit } width={18} height={17}/>
                  </button>}
                  <button className="icon-red" onClick={() => deleteResume(uploadedResume.id, uploadedResume.fileName)}>
                    <img src={ Delete } width={18}/>
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </section>
  );
};

export default Resume;